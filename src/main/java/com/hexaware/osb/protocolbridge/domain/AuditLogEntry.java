package com.hexaware.osb.protocolbridge.domain;

import java.time.Instant;
import java.util.List;

public class AuditLogEntry {
    private String id;
    private String messageId;
    private String correlationId;
    private ProtocolType sourceProtocol;
    private ProtocolType targetProtocol;
    private String sourceEndpoint;
    private String targetEndpoint;
    private String inboundPayloadSnippet;
    private String outboundPayloadSnippet;
    private String responsePayloadSnippet;
    private int statusCode;
    private String status;
    private Instant timestamp;
    private long executionTimeMs;
    private List<String> pipelineTrace;

    public AuditLogEntry() {
        this.timestamp = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public ProtocolType getSourceProtocol() {
        return sourceProtocol;
    }

    public void setSourceProtocol(ProtocolType sourceProtocol) {
        this.sourceProtocol = sourceProtocol;
    }

    public ProtocolType getTargetProtocol() {
        return targetProtocol;
    }

    public void setTargetProtocol(ProtocolType targetProtocol) {
        this.targetProtocol = targetProtocol;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(String sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    public String getInboundPayloadSnippet() {
        return inboundPayloadSnippet;
    }

    public void setInboundPayloadSnippet(String inboundPayloadSnippet) {
        this.inboundPayloadSnippet = inboundPayloadSnippet;
    }

    public String getOutboundPayloadSnippet() {
        return outboundPayloadSnippet;
    }

    public void setOutboundPayloadSnippet(String outboundPayloadSnippet) {
        this.outboundPayloadSnippet = outboundPayloadSnippet;
    }

    public String getResponsePayloadSnippet() {
        return responsePayloadSnippet;
    }

    public void setResponsePayloadSnippet(String responsePayloadSnippet) {
        this.responsePayloadSnippet = responsePayloadSnippet;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public List<String> getPipelineTrace() {
        return pipelineTrace;
    }

    public void setPipelineTrace(List<String> pipelineTrace) {
        this.pipelineTrace = pipelineTrace;
    }
}
