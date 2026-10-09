package com.hexaware.osb.protocolbridge.domain;

public enum ProtocolType {
    HTTP_REST("HTTP/REST (JSON)", "application/json"),
    HTTP_SOAP("HTTP/SOAP (XML)", "text/xml"),
    JMS_QUEUE("JMS Queue (TextMessage)", "application/jms"),
    FILE_SYSTEM("File System (CSV/XML)", "text/plain");

    private final String displayName;
    private final String defaultContentType;

    ProtocolType(String displayName, String defaultContentType) {
        this.displayName = displayName;
        this.defaultContentType = defaultContentType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultContentType() {
        return defaultContentType;
    }
}
