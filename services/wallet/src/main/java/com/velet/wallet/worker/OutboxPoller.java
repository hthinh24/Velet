package com.velet.wallet.worker;

import com.velet.wallet.configuaration.RabbitMQConfig;
import com.velet.wallet.models.Outbox;
import com.velet.wallet.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 5;

    @Scheduled(fixedDelay = 2000)
    public void processOutbox() {
        List<Outbox> batch = fetchAndClaimBatch();
        if (batch.isEmpty()) return;

        List<Long> successIds = new ArrayList<>();
        List<Outbox> failedEvents = new ArrayList<>();

        for (Outbox event : batch) {
            try {
                String routingKey = buildRoutingKey(event);
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.WALLET_EXCHANGE,
                        routingKey,
                        event.getPayload(),
                        message -> {
                            message.getMessageProperties().setMessageId(String.valueOf(event.getId()));
                            return message;
                        }
                );
                successIds.add(event.getId());
            } catch (AmqpException ex) {
                log.warn("Publish failed for outbox id={}, retryCount={}", event.getId(), event.getRetryCount(), ex);
                failedEvents.add(event);
            }
        }

        updateFinalStatuses(successIds, failedEvents);
    }

    private List<Outbox> fetchAndClaimBatch() {
        return transactionTemplate.execute(status -> {
            List<Outbox> batch = outboxRepository.findPendingBatchForUpdate(BATCH_SIZE);
            if (batch.isEmpty()) return batch;

            List<Long> ids = batch.stream().map(Outbox::getId).toList();
            outboxRepository.markAsProcessing(ids);
            return batch;
        });
    }

    private String buildRoutingKey(Outbox event) {
        return (event.getAggregateType() + "." + event.getEventType()).toLowerCase();
    }

    private void updateFinalStatuses(List<Long> successIds, List<Outbox> failedEvents) {
        transactionTemplate.executeWithoutResult(status -> {
            if (!successIds.isEmpty()) {
                outboxRepository.markAsSent(successIds, Instant.now());
            }
            if (!failedEvents.isEmpty()) {
                List<Long> toRetry = failedEvents.stream()
                                                 .filter(e -> e.getRetryCount() + 1 < MAX_RETRY)
                                                 .map(Outbox::getId).toList();
                List<Long> toDeadLetter = failedEvents.stream()
                                                      .filter(e -> e.getRetryCount() + 1 >= MAX_RETRY)
                                                      .map(Outbox::getId).toList();

                if (!toRetry.isEmpty()) outboxRepository.markAsPendingWithRetry(toRetry);
                if (!toDeadLetter.isEmpty()) outboxRepository.markAsFailed(toDeadLetter);
            }
        });
    }
}