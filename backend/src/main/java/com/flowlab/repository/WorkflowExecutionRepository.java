package com.flowlab.repository;

import com.flowlab.domain.WorkflowExecution;
import com.flowlab.domain.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, String> {

    Page<WorkflowExecution> findByStatusOrderByCreatedAtDesc(WorkflowStatus status, Pageable pageable);

    Page<WorkflowExecution> findByWorkflowNameOrderByCreatedAtDesc(String workflowName, Pageable pageable);

    Page<WorkflowExecution> findByWorkflowNameAndStatusOrderByCreatedAtDesc(String workflowName, WorkflowStatus status, Pageable pageable);

    @Query("SELECT DISTINCT e FROM WorkflowExecution e LEFT JOIN FETCH e.steps WHERE e.id = :id")
    Optional<WorkflowExecution> findByIdWithSteps(@Param("id") String id);
}
