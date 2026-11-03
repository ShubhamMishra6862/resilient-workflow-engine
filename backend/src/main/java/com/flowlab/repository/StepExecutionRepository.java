package com.flowlab.repository;

import com.flowlab.domain.StepExecution;
import com.flowlab.domain.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StepExecutionRepository extends JpaRepository<StepExecution, String> {

    @Query("SELECT s FROM StepExecution s WHERE s.execution.id = :executionId AND s.stepName = :stepName")
    Optional<StepExecution> findByExecutionIdAndStepName(@Param("executionId") String executionId, @Param("stepName") String stepName);

    @Query("SELECT s FROM StepExecution s WHERE s.execution.id = :executionId ORDER BY s.stepIndex ASC")
    List<StepExecution> findByExecutionIdOrderByStepIndexAsc(@Param("executionId") String executionId);

    @Query("SELECT s FROM StepExecution s WHERE s.execution.id = :executionId AND s.stepIndex = :stepIndex")
    Optional<StepExecution> findByExecutionIdAndStepIndex(@Param("executionId") String executionId, @Param("stepIndex") Integer stepIndex);

    /**
     * Atomically claim a step lease.
     * Prevents duplicate execution across multiple workers.
     */
    @Modifying
    @Query("UPDATE StepExecution s SET s.status = :targetStatus, s.leaseExpiresAt = :leaseExpiresAt, " +
           "s.startedAt = :now, s.attemptCount = s.attemptCount + 1, s.updatedAt = :now " +
           "WHERE s.id = :stepId AND s.status IN (:eligibleStatuses)")
    int claimStepLease(
            @Param("stepId") String stepId,
            @Param("targetStatus") StepStatus targetStatus,
            @Param("eligibleStatuses") Collection<StepStatus> eligibleStatuses,
            @Param("leaseExpiresAt") Instant leaseExpiresAt,
            @Param("now") Instant now
    );

    /**
     * Finds steps stuck in RUNNING whose lease has expired (e.g. worker died/crashed).
     */
    @Query("SELECT s FROM StepExecution s JOIN FETCH s.execution e WHERE s.status = :status AND s.leaseExpiresAt < :now")
    List<StepExecution> findStuckRunningSteps(
            @Param("status") StepStatus status,
            @Param("now") Instant now
    );

    /**
     * Finds steps whose scheduled retry timestamp has passed.
     */
    @Query("SELECT s FROM StepExecution s JOIN FETCH s.execution e WHERE s.status = :status AND s.nextRetryAt <= :now")
    List<StepExecution> findDueRetrySteps(
            @Param("status") StepStatus status,
            @Param("now") Instant now
    );
}
