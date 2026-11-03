package com.flowlab.repository;

import com.flowlab.domain.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    Optional<WorkflowDefinition> findByNameAndActiveTrue(String name);

    Optional<WorkflowDefinition> findByNameAndVersion(String name, Integer version);

    List<WorkflowDefinition> findAllByOrderByCreatedAtDesc();
}
