package com.hexaware.osb.protocolbridge.exception;

public class ProtocolBridgeException extends RuntimeException {
    private final int statusCode;
    private final String stage;

    public ProtocolBridgeException(String message, String stage, int statusCode) {
        super(message);
        this.stage = stage;
        this.statusCode = statusCode;
    }

    public ProtocolBridgeException(String message, String stage, int statusCode, Throwable cause) {
        super(message, cause);
        this.stage = stage;
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStage() {
        return stage;
    }
}
