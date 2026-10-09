package com.hexaware.osb.protocolbridge.pipeline;

import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * OSB Pipeline Header Transformation Stage:
 * Bridges protocol-specific headers (HTTP Headers, JMS Properties, SOAP Headers, File Attributes).
 */
@Component
public class HeaderTransformStage {

    public void processHeaderTranslation(CanonicalMessage context) {
        ProtocolType source = context.getSourceProtocol();
        ProtocolType target = context.getTargetProtocol();

        // Standard correlation & tracing headers initialization
        if (!context.getHeaders().containsKey("X-Correlation-ID")) {
            context.addHeader("X-Correlation-ID", context.getCorrelationId());
        }
        if (!context.getHeaders().containsKey("X-Message-ID")) {
            context.addHeader("X-Message-ID", context.getMessageId());
        }

        // 1. Source-specific normalization to Canonical Headers
        if (source == ProtocolType.HTTP_REST || source == ProtocolType.HTTP_SOAP) {
            context.addHeader("SourceSystem", context.getHeaders().getOrDefault("X-Source-App", "HTTP-Client"));
        } else if (source == ProtocolType.JMS_QUEUE) {
            context.addHeader("SourceSystem", context.getHeaders().getOrDefault("JMS_SourceSystem", "JMS-Publisher"));
        } else if (source == ProtocolType.FILE_SYSTEM) {
            context.addHeader("SourceSystem", "FileWatcher");
            context.addHeader("FileName", context.getHeaders().getOrDefault("FileName", "input.csv"));
        }

        // 2. Target-specific Protocol Adapter Header Translation
        if (target == ProtocolType.JMS_QUEUE) {
            // Translate canonical headers into JMS Properties
            context.addHeader("JMSCorrelationID", context.getCorrelationId());
            context.addHeader("JMSType", "CanonicalOrderRequest");
            context.addHeader("JMS_OSB_Bridge_Timestamp", String.valueOf(System.currentTimeMillis()));
        } else if (target == ProtocolType.HTTP_SOAP) {
            // Translate headers into SOAP Action & Content-Type
            context.addHeader("SOAPAction", "http://hexaware.com/osb/services/processOrder");
            context.addHeader("Content-Type", "text/xml;charset=UTF-8");
        } else if (target == ProtocolType.HTTP_REST) {
            context.addHeader("Content-Type", "application/json");
            context.addHeader("Accept", "application/json");
        } else if (target == ProtocolType.FILE_SYSTEM) {
            context.addHeader("TargetFileName", "processed_" + context.getCorrelationId() + ".json");
        }
    }
}
