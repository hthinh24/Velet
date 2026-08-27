package com.velet.payment.infrastructure.worker;

import com.velet.payment.configuaration.RabbitMQConfig;
import com.velet.payment.models.Outbox;
import com.velet.payment.repository.OutboxRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TransactionTemplate transactionTemplate;
    private final Propagator propagator;
    private final Tracer tracer;

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
                Map<String, String> carrier = new HashMap<>();
                if (event.getTraceParent() != null) {
                    carrier.put("traceparent", event.getTraceParent());
                }

                long createdAtNanos = event.getCreatedAt().toEpochMilli() * 1_000_000L;
                long nowNanos = System.currentTimeMillis() * 1_000_000L;

                Span.Builder waitSpanBuilder = propagator.extract(carrier, Map::get);
                Span waitSpan = waitSpanBuilder
                        .name("outbox.wait_time")
                        .startTimestamp(createdAtNanos, TimeUnit.NANOSECONDS)
                        .start();
                waitSpan.end(nowNanos, TimeUnit.NANOSECONDS);

                Span publishSpan = tracer.spanBuilder()
                                         .name("outbox.publish")
                                         .setParent(waitSpan.context())
                                         .start();

                log.info("DEBUG span created, traceId={} (expected from stored={})",
                         publishSpan.context().traceId(), event.getTraceParent());

                try (Tracer.SpanInScope ws = tracer.withSpan(publishSpan)) {
                    String routingKey = buildRoutingKey(event);

                    MessageProperties props = new MessageProperties();
                    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    props.setMessageId(String.valueOf(event.getId()));
                    props.getHeaders().put("__TypeId__", event.getEventType());

                    Message message = new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), props);

                    rabbitTemplate.send(RabbitMQConfig.PAYMENT_EXCHANGE, routingKey, message);

                    successIds.add(event.getId());
                    log.debug("outbox.published id={} routingKey={}", event.getId(), routingKey);

                } finally {
                    publishSpan.end();
                }

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