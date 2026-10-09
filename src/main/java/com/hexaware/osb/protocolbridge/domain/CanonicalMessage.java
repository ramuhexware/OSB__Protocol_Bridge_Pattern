package com.hexaware.osb.protocolbridge.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Normalized internal message model representing OSB pipeline context ($body, $header, $inbound, $outbound).
 */
public class CanonicalMessage {
    private String messageId;
    private String correlationId;
    private ProtocolType sourceProtocol;
    private ProtocolType targetProtocol;
    private String rawInboundPayload;
    private String transformedPayload;
    private String rawOutboundPayload;
    private String responsePayload;
    private String sourceEndpoint;
    private String targetEndpoint;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> properties = new HashMap<>();
    private Instant timestamp;
    private int statusCode = 200;
    private String statusMessage = "SUCCESS";

    public CanonicalMessage() {
        this.messageId = "MSG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.correlationId = "CORR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.timestamp = Instant.now();
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

    public String getRawInboundPayload() {
        return rawInboundPayload;
    }

    public void setRawInboundPayload(String rawInboundPayload) {
        this.rawInboundPayload = rawInboundPayload;
    }

    public String getTransformedPayload() {
        return transformedPayload;
    }

    public void setTransformedPayload(String transformedPayload) {
        this.transformedPayload = transformedPayload;
    }

    public String getRawOutboundPayload() {
        return rawOutboundPayload;
    }

    public void setRawOutboundPayload(String rawOutboundPayload) {
        this.rawOutboundPayload = rawOutboundPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
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

    @Override
    public String toString() {
        return "CanonicalMessage{" +
                "messageId='" + messageId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", sourceProtocol=" + sourceProtocol +
                ", targetProtocol=" + targetProtocol +
                ", sourceEndpoint='" + sourceEndpoint + '\'' +
                ", targetEndpoint='" + targetEndpoint + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
