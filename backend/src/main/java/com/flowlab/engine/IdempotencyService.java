package com.flowlab.engine;

import com.flowlab.domain.IdempotencyRecord;
import com.flowlab.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public static String buildKey(String executionId, String stepName) {
        return executionId + ":" + stepName;
    }

    @Transactional(readOnly = true)
    public Optional<String> getResult(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey)
                .map(IdempotencyRecord::getResultJson);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResult(String idempotencyKey, String aggregateId, String stepName, String resultJson) {
        try {
            IdempotencyRecord record = new IdempotencyRecord(idempotencyKey, aggregateId, stepName, resultJson);
            repository.save(record);
        } catch (Exception e) {
            // Already exists; ignore to preserve idempotency
        }
    }
}
