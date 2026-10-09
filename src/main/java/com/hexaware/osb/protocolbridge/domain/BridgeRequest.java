package com.hexaware.osb.protocolbridge.domain;

import java.util.HashMap;
import java.util.Map;

public class BridgeRequest {
    private ProtocolType sourceProtocol;
    private ProtocolType targetProtocol;
    private String payload;
    private String targetEndpoint;
    private Map<String, String> headers = new HashMap<>();

    public BridgeRequest() {}

    public BridgeRequest(ProtocolType sourceProtocol, ProtocolType targetProtocol, String payload, String targetEndpoint) {
        this.sourceProtocol = sourceProtocol;
        this.targetProtocol = targetProtocol;
        this.payload = payload;
        this.targetEndpoint = targetEndpoint;
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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
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
}
