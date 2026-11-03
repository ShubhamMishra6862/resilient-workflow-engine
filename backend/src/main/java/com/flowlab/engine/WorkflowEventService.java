package com.flowlab.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowlab.domain.WorkflowEvent;
import com.flowlab.domain.WorkflowEventType;
import com.flowlab.repository.WorkflowEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowEventService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEventService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WorkflowEventRepository eventRepository;

    public WorkflowEventService(WorkflowEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String executionId, WorkflowEventType eventType, String stepName, Object detail) {
        String detailJson = null;
        if (detail != null) {
            try {
                detailJson = detail instanceof String ? (String) detail : OBJECT_MAPPER.writeValueAsString(detail);
            } catch (Exception e) {
                detailJson = String.valueOf(detail);
            }
        }

        try {
            WorkflowEvent event = new WorkflowEvent(executionId, eventType, stepName, detailJson);
            eventRepository.save(event);
            log.info("[AUDIT] executionId={} event={} step={} detail={}", executionId, eventType, stepName, detailJson);
        } catch (Exception e) {
            log.error("Failed to persist workflow event for executionId={}", executionId, e);
        }
    }
}
