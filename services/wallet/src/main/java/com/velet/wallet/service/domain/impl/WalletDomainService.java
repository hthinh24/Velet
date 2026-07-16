package com.velet.wallet.service.domain.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.wallet.app.SystemAccountCache;
import com.velet.wallet.dto.request.ConfirmReservationRequest;
import com.velet.wallet.dto.request.ReleaseBalanceRequest;
import com.velet.wallet.dto.request.ReserveBalanceRequest;
import com.velet.wallet.dto.request.TransferRequest;
import com.velet.wallet.dto.response.*;
import com.velet.wallet.exception.AppException;
import com.velet.wallet.exception.ErrorCode;
import com.velet.wallet.infrastructure.consumer.wallet.event.*;
import com.velet.wallet.models.*;
import com.velet.wallet.models.enums.*;
import com.velet.wallet.repository.LedgerRepository;
import com.velet.wallet.repository.OutboxRepository;
import com.velet.wallet.repository.TransactionRepository;
import com.velet.wallet.repository.WalletRepository;
import com.velet.wallet.service.domain.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles the core DB writes for a transfer in a single atomic transaction.
 * Only contain business logics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletDomainService implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final SystemAccountCache systemAccountCache;

    @Override
    public WalletInfo getWalletById(String walletId) {
        Wallet wallet = walletRepository.findById(Long.parseLong(walletId))
                                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        if (wallet.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ErrorCode.WALLET_INACTIVE);
        }

        BalanceComponents balance = getBalanceComponents(walletId);

        WalletInfo info = new WalletInfo(
                wallet.getId(),
                wallet.getOwnerId(),
                wallet.getType(),
                balance.available(),
                balance.pendingDebits(),
                wallet.getCurrency(),
                wallet.getStatus()
        );

        return info;
    }

    @Override
    public BalanceComponents getBalanceComponents(String walletId) {
        WalletRepository.BalanceRow row = walletRepository.computeBalanceRaw(Long.parseLong(walletId));
        BalanceComponents balance = new BalanceComponents(
                row.getPostedDebits(), row.getPostedCredits(),
                row.getPendingDebits(), row.getPendingCredits()
        );

        return balance;
    }

    @Override
    public WalletBalanceResponse getWalletBalance(Long walletId) {
        WalletInfo walletInfo = getWalletById(String.valueOf(walletId));
        return new WalletBalanceResponse(
                walletInfo.availableBalance(),
                walletInfo.status().name()
        );
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        Long receiverId = Long.parseLong(request.toWalletId());
        Long senderId = Long.parseLong(request.fromWalletId());

        long amountInCents = request.amount().longValueExact();

        // Acquire pessimistic write lock on both wallet rows to prevent race condition
        walletRepository.findByIdWithLock(receiverId)
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        walletRepository.findByIdWithLock(senderId)
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        WalletRepository.BalanceRow row = walletRepository.computeBalanceRaw(senderId);
        BalanceComponents balance = new BalanceComponents(
                row.getPostedDebits(), row.getPostedCredits(),
                row.getPendingDebits(), row.getPendingCredits()
        );
        if (balance.available() < amountInCents) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        Wallet senderWallet = walletRepository.getReferenceById(senderId);
        Wallet creditWallet = walletRepository.getReferenceById(receiverId);

        // 5c. Insert Transaction — saveAndFlush forces the INSERT immediately
        // can catch DataIntegrityViolationException (duplicate idempotencyKey)
        // and return as soon as possible
        Transaction transaction;
        try {
            transaction = transactionRepository.saveAndFlush(Transaction.builder()
                                                                        .sourceWallet(senderWallet)
                                                                        .destinationWallet(creditWallet)
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
        ledgerRepository.saveAll(List.of(
                LedgerEntry.builder()
                           .transaction(transaction)
                           .wallet(senderWallet)
                           .entryType(EntryType.DEBIT)
                           .amount(amountInCents)
                           .status(LedgerEntryStatus.POSTED)
                           .idempotencyKey(buildIdempotentKey(request.idempotencyKey(), "debit"))
                           .build(),
                LedgerEntry.builder()
                           .transaction(transaction)
                           .wallet(creditWallet)
                           .entryType(EntryType.CREDIT)
                           .amount(amountInCents)
                           .status(LedgerEntryStatus.POSTED)
                           .idempotencyKey(buildIdempotentKey(request.idempotencyKey(), "credit"))
                           .build()
        ));

        // 5f. Create Outbox TRANSFER_COMPLETED
        List<Outbox> events = new ArrayList<>();
        events.add(
                Outbox.builder()
                      .aggregateId(transaction.getId())
                      .aggregateType(AggregateType.TRANSACTION)
                      .eventType(EventType.TRANSFER_COMPLETED)
                      .payload(buildTransferCompletedPayload(transaction, request, now))
                      .build()
        );

        // 5g. Create Outbox LOYALTY_TRANSFER_EVENT — only when loyalty data is present
        if (request.voucherId() != null || request.points() != null) {
            events.add(
                    Outbox.builder()
                          .aggregateId(transaction.getId())
                          .aggregateType(AggregateType.TRANSACTION)
                          .eventType(EventType.LOYALTY_TRANSFER_EVENT)
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

    @Override
    @Transactional
    public ReserveBalanceResponse reserve(ReserveBalanceRequest request) {
        String debitKey = buildIdempotentKey(request.idempotencyKey(), "debit");
        String creditKey = buildIdempotentKey(request.idempotencyKey(), "credit");

        Long sourceWalletId = Long.parseLong(request.fromWalletId());
        Long holdWalletId = Long.parseLong(request.toWalletId());
        Long amountInCents = request.amount().longValueExact();

        walletRepository.findByIdWithLock(sourceWalletId)
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        walletRepository.findByIdWithLock(holdWalletId)
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        WalletRepository.BalanceRow row = walletRepository.computeBalanceRaw(sourceWalletId);
        BalanceComponents balance = new BalanceComponents(
                row.getPostedDebits(), row.getPostedCredits(),
                row.getPendingDebits(), row.getPendingCredits()
        );
        log.info("reserveBalance: walletId={}, available={}, amount={}", sourceWalletId, balance.available(),
                 amountInCents);
        if (balance.available() < amountInCents) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        ReserveBalanceResponse reserveExisted = lookupExistingTransaction(request.idempotencyKey());
        if (reserveExisted != null) {
            return reserveExisted;
        }

        Wallet sourceWallet = walletRepository.getReferenceById(sourceWalletId);
        Wallet holdWallet = walletRepository.getReferenceById(holdWalletId);

        Transaction transaction;
        transaction = transactionRepository.save(Transaction.builder()
                                                            .sourceWallet(sourceWallet)
                                                            .destinationWallet(holdWallet)
                                                            .amount(amountInCents)
                                                            .type(TransactionType.PAYMENT)
                                                            .status(TransactionStatus.PENDING)
                                                            .idempotencyKey(request.idempotencyKey())
                                                            .build());

        ledgerRepository.save(LedgerEntry.builder()
                                         .transaction(transaction)
                                         .wallet(sourceWallet)
                                         .entryType(EntryType.DEBIT)
                                         .amount(amountInCents)
                                         .status(LedgerEntryStatus.PENDING)
                                         .idempotencyKey(debitKey)
                                         .build());

        Long suspendWalletId = systemAccountCache.resolve(AccountType.SUSPENSE_ACCOUNT);
        Wallet suspendWallet = walletRepository.getReferenceById(suspendWalletId);
        ledgerRepository.save(LedgerEntry.builder()
                                         .transaction(transaction)
                                         .wallet(suspendWallet)
                                         .entryType(EntryType.CREDIT)
                                         .amount(amountInCents)
                                         .status(LedgerEntryStatus.PENDING)
                                         .idempotencyKey(creditKey)
                                         .build());

        Instant now = Instant.now();
        Outbox outbox = Outbox.builder()
                              .aggregateId(transaction.getId())
                              .aggregateType(AggregateType.TRANSACTION)
                              .eventType(EventType.BALANCE_RESERVATION_CREATED)
                              .payload(buildBalanceReservationCreatedEvent(transaction, request, now))
                              .build();
        outboxRepository.save(outbox);

        return new ReserveBalanceResponse(ReservationStatus.RESERVED, transaction.getId(), request.idempotencyKey(),
                                          transaction.getCreatedAt().toEpochMilli(), null);
    }

    @Override
    public ReserveBalanceResponse getReservationStatus(String idempotencyKey) {
        return null;
    }

    @Transactional
    public ConfirmReservationResponse confirmReservation(ConfirmReservationRequest request) {
        String debitKey = buildIdempotentKey(request.originIdempotencyKey(), "debit");
        String creditKey = buildIdempotentKey(request.originIdempotencyKey(), "credit");

        log.info("confirmReservation: originIdempotencyKey={}, confirmIdempotencyKey={}",
                 request.originIdempotencyKey(), request.confirmIdempotencyKey());

        Transaction transaction = transactionRepository.findByIdempotencyKey(request.originIdempotencyKey())
                                                       .orElseThrow(
                                                               () -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));

        if (transaction.getStatus() == TransactionStatus.SUCCESS) {
            return new ConfirmReservationResponse(ReservationStatus.COMPLETED,
                                                  transaction.getId(),
                                                  request.originIdempotencyKey(),
                                                  transaction.getCreatedAt().toEpochMilli(),
                                                  transaction.getUpdatedAt().toEpochMilli());
        }

        if (transaction.getStatus() == TransactionStatus.FAILED) {
            throw new AppException(ErrorCode.INVALID_RESERVATION_STATE);
        }

        List<LedgerEntry> pairEntries = ledgerRepository
                .findByIdempotencyKeys(List.of(debitKey, creditKey));
        if (pairEntries.size() != 2) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        pairEntries.forEach(entry -> {
            if (entry.getStatus() != LedgerEntryStatus.PENDING) {
                throw new AppException(ErrorCode.INVALID_RESERVATION_STATE);
            }
        });

        pairEntries.forEach(entry -> {
            ledgerRepository.updateLedgerEntryStatus(entry.getId(), LedgerEntryStatus.POSTED);
        });

        transaction.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(transaction);

        Wallet debitWallet = pairEntries.get(1).getWallet();
        Wallet creditWallet = transaction.getDestinationWallet();
        LedgerEntry debitEntry = LedgerEntry.builder()
                                            .transaction(transaction)
                                            .wallet(debitWallet)
                                            .entryType(EntryType.DEBIT)
                                            .amount(transaction.getAmount())
                                            .status(LedgerEntryStatus.POSTED)
                                            .idempotencyKey(
                                                    buildIdempotentKey(request.confirmIdempotencyKey(), "debit"))
                                            .build();
        LedgerEntry creditEntry = LedgerEntry.builder()
                                             .transaction(transaction)
                                             .wallet(creditWallet)
                                             .entryType(EntryType.CREDIT)
                                             .amount(transaction.getAmount())
                                             .status(LedgerEntryStatus.POSTED)
                                             .idempotencyKey(buildIdempotentKey(request.confirmIdempotencyKey(),
                                                                                      "credit"))
                                             .build();
        ledgerRepository.saveAll(List.of(debitEntry, creditEntry));

        Instant now = Instant.now();
        Outbox outbox = Outbox.builder()
                              .aggregateId(transaction.getId())
                              .aggregateType(AggregateType.TRANSACTION)
                              .eventType(EventType.RESERVATION_CONFIRMED)
                              .payload(buildReservationConfirmedEvent(transaction, request, now))
                              .build();
        outboxRepository.save(outbox);

        log.info("confirmReservation completed: transactionId={}, confirmIdempotencyKey={}",
                 transaction.getId(), request.confirmIdempotencyKey());

        return new ConfirmReservationResponse(ReservationStatus.COMPLETED, transaction.getId(),
                                              request.originIdempotencyKey(), transaction.getCreatedAt().toEpochMilli(),
                                              transaction.getUpdatedAt().toEpochMilli());
    }

    @Transactional
    public ReleaseBalanceResponse release(ReleaseBalanceRequest request) {
        String debitKey = buildIdempotentKey(request.originIdempotencyKey(), "debit");
        String creditKey = buildIdempotentKey(request.originIdempotencyKey(), "credit");

        log.info("releaseBalance: originIdempotencyKey={}, releaseIdempotencyKey={}", request.originIdempotencyKey(),
                 request.releaseIdempotencyKey());

        Transaction transaction = transactionRepository.findByIdempotencyKey(request.originIdempotencyKey())
                                                       .orElseThrow(
                                                               () -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));
        if (transaction.getStatus() == TransactionStatus.FAILED &&
            transaction.getCancelReason().equals(CancelReason.USER_CANCELLED)) {

            return new ReleaseBalanceResponse(ReservationStatus.RELEASED,
                                              transaction.getId(),
                                              request.originIdempotencyKey(),
                                              transaction.getCreatedAt().toEpochMilli(),
                                              transaction.getUpdatedAt().toEpochMilli());
        }

        ledgerRepository.findByIdempotencyKey(request.releaseIdempotencyKey() + "DEBIT")
                        .ifPresent(entry -> {
                            throw new AppException(ErrorCode.DUPLICATE_RELEASE);
                        });

        List<LedgerEntry> pairEntries = ledgerRepository
                .findByIdempotencyKeys(List.of(debitKey, creditKey));
        if (pairEntries.size() != 2) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND);
        }

        pairEntries.forEach(entry -> {
            if (entry.getStatus() != LedgerEntryStatus.PENDING) {
                throw new AppException(ErrorCode.INVALID_RESERVATION_STATE);
            }
        });

        pairEntries.forEach(entry -> {
            ledgerRepository.updateLedgerEntryStatus(entry.getId(), LedgerEntryStatus.POSTED);
        });

        // Compensation
        Wallet sourceWallet = transaction.getSourceWallet();
        Wallet destinationWallet = transaction.getDestinationWallet();
        Long amountInCents = transaction.getAmount();

        walletRepository.findByIdWithLock(sourceWallet.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));
        walletRepository.findByIdWithLock(destinationWallet.getId())
                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setCancelReason(request.reason());
        transactionRepository.saveAndFlush(transaction);

        Long suspendWalletId = systemAccountCache.resolve(AccountType.SUSPENSE_ACCOUNT);
        Wallet suspendWallet = walletRepository.getReferenceById(suspendWalletId);
        ledgerRepository.save(LedgerEntry.builder()
                                         .transaction(transaction)
                                         .wallet(suspendWallet)
                                         .entryType(EntryType.DEBIT)
                                         .amount(amountInCents)
                                         .status(LedgerEntryStatus.POSTED)
                                         .idempotencyKey(request.releaseIdempotencyKey() + "DEBIT")
                                         .build());

        ledgerRepository.save(LedgerEntry.builder()
                                         .transaction(transaction)
                                         .wallet(sourceWallet)
                                         .entryType(EntryType.CREDIT)
                                         .amount(amountInCents)
                                         .status(LedgerEntryStatus.POSTED)
                                         .idempotencyKey(request.releaseIdempotencyKey() + "CREDIT")
                                         .build());

        Instant now = Instant.now();
        Outbox outbox = Outbox.builder()
                              .aggregateId(transaction.getId())
                              .aggregateType(AggregateType.TRANSACTION)
                              .eventType(EventType.TRANSACTION_CANCELLED)
                              .payload(buildTransactionCanceledEvent(transaction.getId().toString(),
                                                                     sourceWallet.getId().toString(),
                                                                     destinationWallet.getId().toString(),
                                                                     BigDecimal.valueOf(transaction.getAmount()), now))
                              .build();
        outboxRepository.save(outbox);

        log.info("Release balance completed: transactionId={}, releaseIdempotencyKey={}", transaction.getId(),
                 request.releaseIdempotencyKey());

        return new ReleaseBalanceResponse(ReservationStatus.RELEASED, transaction.getId(),
                                          request.originIdempotencyKey(), transaction.getCreatedAt().toEpochMilli(),
                                          transaction.getUpdatedAt().toEpochMilli());
    }


    @Override
    public void validateWalletOwner(Long walletId, Long userId) {
        Wallet wallet = walletRepository.findById(walletId)
                                        .orElseThrow(() -> new AppException(ErrorCode.WALLET_NOT_FOUND));

        log.info("validateWalletOwner: walletId={}, walletOwnerId={}, userId={}", walletId, wallet.getOwnerId(),
                 userId);
        if (!Objects.equals(wallet.getOwnerId(), userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACTION);
        }
    }

    @Override
    public void postInternalEntry(Long debitWalletId, Long creditWalletId, TransactionType transactionType,
                                  Long amount, String idempotencyKey) {

        Wallet debitWallet = walletRepository.getReferenceById(debitWalletId);
        Wallet creditWallet = walletRepository.getReferenceById(creditWalletId);

        Transaction transaction;
        try {
            transaction = transactionRepository.saveAndFlush(Transaction.builder()
                                                                        .sourceWallet(debitWallet)
                                                                        .destinationWallet(creditWallet)
                                                                        .amount(amount)
                                                                        .type(transactionType)
                                                                        .status(TransactionStatus.SUCCESS)
                                                                        .idempotencyKey(idempotencyKey)
                                                                        .build());
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.DUPLICATE_TRANSFER);
        }

        ledgerRepository.saveAll(List.of(
                LedgerEntry.builder()
                           .transaction(transaction)
                           .wallet(debitWallet)
                           .entryType(EntryType.DEBIT)
                           .amount(amount)
                           .status(LedgerEntryStatus.POSTED)
                           .idempotencyKey(buildIdempotentKey(idempotencyKey, "debit"))
                           .build(),
                LedgerEntry.builder()
                           .transaction(transaction)
                           .wallet(creditWallet)
                           .entryType(EntryType.CREDIT)
                           .amount(amount)
                           .status(LedgerEntryStatus.POSTED)
                           .idempotencyKey(buildIdempotentKey(idempotencyKey, "credit"))
                           .build()
        ));
    }

    private ReserveBalanceResponse lookupExistingTransaction(String idempotencyKey) {
        Transaction existing = transactionRepository.findByIdempotencyKey(idempotencyKey)
                                                    .orElse(null);

        if (existing == null) {
            return null;
        }

        return switch (existing.getStatus()) {
            case PENDING -> new ReserveBalanceResponse(ReservationStatus.RESERVED, existing.getId(),
                                                       existing.getIdempotencyKey(),
                                                       existing.getCreatedAt().toEpochMilli(),
                                                       null);
            case SUCCESS -> new ReserveBalanceResponse(ReservationStatus.COMPLETED, existing.getId(),
                                                       existing.getIdempotencyKey(),
                                                       existing.getCreatedAt().toEpochMilli(),
                                                       null);
            case FAILED -> new ReserveBalanceResponse(ReservationStatus.RELEASED, existing.getId(),
                                                      existing.getIdempotencyKey(),
                                                      existing.getCreatedAt().toEpochMilli(),
                                                      existing.getUpdatedAt().toEpochMilli());
        };
    }

    private String buildIdempotentKey(String... suffixs) {
        StringBuilder sb = new StringBuilder();
        for (String suffix : suffixs) {
            if (suffix == null || suffix.isEmpty()) {
                throw new IllegalArgumentException("suffix must not be empty");
            }
            sb.append(suffix).append(":");
        }
        return sb.toString();
    }

    private String buildTransferCompletedPayload(Transaction transaction, TransferRequest request, Instant occurredAt) {
        try {
            var event = new TransferCompletedEvent(
                    String.valueOf(transaction.getId()),
                    Long.parseLong(request.fromWalletId()),
                    Long.parseLong(request.toWalletId()),
                    request.amount(),
                    transaction.getCurrency(),
                    occurredAt.toString()
            );
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildLoyaltyEventPayload(Transaction transaction, TransferRequest request, Instant occurredAt) {
        try {
            var event = new LoyaltyEvent(
                    String.valueOf(transaction.getId()),
                    Long.parseLong(request.fromWalletId()),
                    request.voucherId(),
                    request.points(),
                    occurredAt.toString()
            );
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildBalanceReservationCreatedEvent(Transaction transaction, ReserveBalanceRequest request,
                                                       Instant occurredAt) {
        try {
            var event = new BalanceReservationCreatedEvent(
                    String.valueOf(transaction.getId()),
                    Long.parseLong(request.fromWalletId()),
                    Long.parseLong(request.toWalletId()),
                    request.amount(),
                    transaction.getCurrency(),
                    occurredAt.toString()
            );
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildTransactionCanceledEvent(String transactionId, String fromWalletId, String toWalletId,
                                                 BigDecimal amount,
                                                 Instant occurredAt) {
        try {
            var event = new TransactionCancelledEvent(
                    transactionId,
                    fromWalletId,
                    toWalletId,
                    amount,
                    occurredAt.toString()
            );
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildReservationConfirmedEvent(Transaction transaction, ConfirmReservationRequest request,
                                                  Instant occurredAt) {
        try {
            ReservationConfirmedEvent reservationConfirmedEvent =
                    new ReservationConfirmedEvent(
                            transaction.getId().toString(),
                            transaction.getSourceWallet().getId().toString(),
                            transaction.getDestinationWallet().getId().toString(),
                            BigDecimal.valueOf(transaction.getAmount()),
                            occurredAt.toString()
                    );
            return objectMapper.writeValueAsString(reservationConfirmedEvent);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
