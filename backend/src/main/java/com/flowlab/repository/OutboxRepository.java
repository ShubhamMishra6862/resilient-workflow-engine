package com.flowlab.repository;

import com.flowlab.domain.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    @Query("SELECT o FROM OutboxMessage o WHERE o.status = :status ORDER BY o.createdAt ASC")
    List<OutboxMessage> findPendingMessages(@Param("status") String status);
}
