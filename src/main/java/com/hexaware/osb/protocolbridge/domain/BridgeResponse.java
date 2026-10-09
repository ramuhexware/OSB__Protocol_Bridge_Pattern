package com.hexaware.osb.protocolbridge.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BridgeResponse {
    private String messageId;
    private String correlationId;
    private ProtocolType sourceProtocol;
    private ProtocolType targetProtocol;
    private int statusCode;
    private String statusMessage;
    private String responsePayload;
    private Map<String, String> responseHeaders;
    private long executionTimeMs;
    private List<String> pipelineTrace = new ArrayList<>();

    public BridgeResponse() {}

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

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
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

    public void addPipelineTraceStep(String step) {
        this.pipelineTrace.add(step);
    }
}
