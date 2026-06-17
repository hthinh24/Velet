package com.velet.wallet.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.dto.response.TransferResponse;
import com.velet.wallet.exception.AppException;
import com.velet.wallet.exception.ErrorCode;
import com.velet.wallet.models.Wallet;
import com.velet.wallet.models.LedgerEntry;
import com.velet.wallet.models.Outbox;
import com.velet.wallet.models.Transaction;
import com.velet.wallet.models.enums.EntryType;
import com.velet.wallet.models.enums.TransactionStatus;
import com.velet.wallet.models.enums.TransactionType;
import com.velet.wallet.repository.LedgerRepository;
import com.velet.wallet.repository.OutboxRepository;
import com.velet.wallet.repository.TransactionRepository;
import com.velet.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the core DB writes for a transfer in a single atomic transaction.
 * Only contain business logics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceExecutor {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransferResponse transfer(WalletInfo sender, WalletInfo receiver, TransferRequest request) {
        long amountInCents = request.amount().longValueExact();

        // 5a & 5b. Update Credit/Debit wallet
        walletRepository.deductBalance(sender.walletId(), amountInCents);
        walletRepository.addBalance(receiver.walletId(), amountInCents);

        Wallet senderWallet = walletRepository.getReferenceById(sender.walletId());
        Wallet receiverWallet = walletRepository.getReferenceById(receiver.walletId());

        // 5c. Insert Transaction — saveAndFlush forces the INSERT immediately
        // can catch DataIntegrityViolationException (duplicate idempotencyKey)
        // and return as soon as possible
        Transaction transaction;
        try {
            transaction = transactionRepository.saveAndFlush(Transaction.builder()
                    .sourceWallet(senderWallet)
                    .destinationWallet(receiverWallet)
                    .amount(amountInCents)
                    .type(TransactionType.TRANSFER)
                    .status(TransactionStatus.SUCCESS)
                    .idempotencyKey(request.idempotencyKey())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.DUPLICATE_TRANSFER);
        }

        Instant now = Instant.now();

        // 5d. Insert LedgerEntry DEBIT (sender) 
        // 5e. Insert LedgerEntry CREDIT (receiver)
        Long senderNewBalance = senderWallet.getAvailableBalance() - senderWallet.getPendingBalance() - amountInCents;
        Long receiverNewBalance = receiverWallet.getAvailableBalance() - receiverWallet.getPendingBalance() + amountInCents;

        ledgerRepository.saveAll(List.of(
            LedgerEntry.builder()
                .transaction(transaction)
                .wallet(senderWallet)
                .entryType(EntryType.DEBIT)
                .amount(amountInCents)
                .runningBalance(senderNewBalance)
                .idempotencyKey(generateLedgerIdempotentKey(request.idempotencyKey(), "debit"))
                .build(),
            LedgerEntry.builder()
                .transaction(transaction)
                .wallet(receiverWallet)
                .entryType(EntryType.CREDIT)
                .amount(amountInCents)
                .runningBalance(receiverNewBalance)
                .idempotencyKey(generateLedgerIdempotentKey(request.idempotencyKey(), "credit"))
                .build()
            ));

        // 5f. Create Outbox TRANSFER_COMPLETED
        List<Outbox> events = List.of(
            Outbox.builder()
                .aggregateId(transaction.getId())
                .aggregateType("Transaction")
                .eventType("TRANSFER_COMPLETED")
                .payload(buildTransferCompletedPayload(transaction, request, now))
                .build()
        );

        // 5g. Create Outbox LOYALTY_TRANSFER_EVENT — only when loyalty data is present
        if (request.voucherId() != null || request.points() != null) {
            events.add(
                Outbox.builder()
                    .aggregateId(transaction.getId())
                    .aggregateType("Transaction")
                    .eventType("LOYALTY_TRANSFER_EVENT")
                    .payload(buildLoyaltyEventPayload(transaction, request, now))
                    .build()
            );
        }

        // 5h. Save all events to outbox
        outboxRepository.saveAll(events);

        return new TransferResponse(
                String.valueOf(transaction.getId()),
                request.fromWalletId(),
                request.toWalletId(),
                request.amount(),
                TransactionStatus.SUCCESS,
                now
        );
    }

    private String generateLedgerIdempotentKey(String idempotencyKey, String suffix) {
        return UUID.nameUUIDFromBytes(
                (idempotencyKey + ":" + suffix).getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    private String buildTransferCompletedPayload(Transaction transaction, TransferRequest request, Instant occurredAt) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "transactionId", String.valueOf(transaction.getId()),
                    "fromWalletId",   request.fromWalletId(),
                    "toWalletId",     request.toWalletId(),
                    "amount",        request.amount(),
                    "currency",      transaction.getCurrency(),
                    "occurredAt",    occurredAt.toString()
            ));
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildLoyaltyEventPayload(Transaction transaction, TransferRequest request, Instant occurredAt) {
        try {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("transactionId", String.valueOf(transaction.getId()));
            payload.put("userId",        request.fromWalletId());
            if (request.voucherId() != null) payload.put("voucherId", request.voucherId());
            if (request.points()    != null) payload.put("points",    request.points());
            payload.put("occurredAt", occurredAt.toString());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
