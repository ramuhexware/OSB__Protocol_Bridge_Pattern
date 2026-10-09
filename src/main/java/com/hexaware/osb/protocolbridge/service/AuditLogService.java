package com.hexaware.osb.protocolbridge.service;

import com.hexaware.osb.protocolbridge.domain.AuditLogEntry;
import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AuditLogService {

    private final ConcurrentLinkedDeque<AuditLogEntry> auditLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_LOGS = 100;

    public void recordTransaction(CanonicalMessage context, BridgeResponse response) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.setId("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entry.setMessageId(context.getMessageId());
        entry.setCorrelationId(context.getCorrelationId());
        entry.setSourceProtocol(context.getSourceProtocol());
        entry.setTargetProtocol(context.getTargetProtocol());
        entry.setSourceEndpoint(context.getSourceEndpoint());
        entry.setTargetEndpoint(context.getTargetEndpoint());
        
        entry.setInboundPayloadSnippet(truncate(context.getRawInboundPayload(), 250));
        entry.setOutboundPayloadSnippet(truncate(context.getTransformedPayload(), 250));
        entry.setResponsePayloadSnippet(truncate(response.getResponsePayload(), 250));

        entry.setStatusCode(response.getStatusCode());
        entry.setStatus(response.getStatusMessage());
        entry.setExecutionTimeMs(response.getExecutionTimeMs());
        entry.setPipelineTrace(new ArrayList<>(response.getPipelineTrace()));

        auditLogs.addFirst(entry);
        while (auditLogs.size() > MAX_LOGS) {
            auditLogs.pollLast();
        }
    }

    public List<AuditLogEntry> getAuditLogs() {
        return Collections.unmodifiableList(new ArrayList<>(auditLogs));
    }

    public void clearLogs() {
        auditLogs.clear();
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "... [truncated]";
    }
}
