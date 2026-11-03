package com.flowlab.repository;

import com.flowlab.domain.WorkflowEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, Long> {

    List<WorkflowEvent> findByExecutionIdOrderByCreatedAtAsc(String executionId);
}
