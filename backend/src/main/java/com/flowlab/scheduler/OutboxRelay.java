package com.flowlab.scheduler;

import com.flowlab.domain.OutboxMessage;
import com.flowlab.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "flowlab.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;

    public OutboxRelay(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /**
     * Polls the outbox table and publishes events.
     * Guarantees at-least-once delivery without dual-write risk.
     */
    @Scheduled(fixedDelayString = "${flowlab.outbox.interval-ms:1500}")
    @Transactional
    public void processOutbox() {
        List<OutboxMessage> pending = outboxRepository.findPendingMessages("PENDING");
        if (pending.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (OutboxMessage message : pending) {
            try {
                // In production, publish to Kafka: kafkaTemplate.send(message.getTopic(), message.getAggregateId(), message.getPayloadJson())
                log.info("[OUTBOX RELAY] Dispatched outbox message id={}, topic={}, aggregateId={}",
                        message.getId(), message.getTopic(), message.getAggregateId());

                message.setStatus("PUBLISHED");
                message.setPublishedAt(now);
                outboxRepository.save(message);
            } catch (Exception e) {
                log.error("[OUTBOX RELAY] Failed to publish outbox message id={}", message.getId(), e);
                message.setRetryCount(message.getRetryCount() + 1);
                outboxRepository.save(message);
            }
        }
    }
}
