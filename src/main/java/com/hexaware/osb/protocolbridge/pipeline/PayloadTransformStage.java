package com.hexaware.osb.protocolbridge.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.exception.ProtocolBridgeException;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;

/**
 * OSB Pipeline Payload Transformation Stage:
 * Transforms payload between REST JSON, SOAP XML Envelopes, JMS Messages, and File Formats.
 */
@Component
public class PayloadTransformStage {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processPayloadTransformation(CanonicalMessage context) {
        String inputPayload = context.getRawInboundPayload();
        ProtocolType source = context.getSourceProtocol();
        ProtocolType target = context.getTargetProtocol();

        if (inputPayload == null || inputPayload.trim().isEmpty()) {
            inputPayload = "{}";
        }

        try {
            String transformed;

            if (source == ProtocolType.HTTP_REST && target == ProtocolType.HTTP_SOAP) {
                transformed = jsonToSoapEnvelope(inputPayload, context.getCorrelationId());
            } else if (source == ProtocolType.HTTP_SOAP && target == ProtocolType.HTTP_REST) {
                transformed = soapEnvelopeToJson(inputPayload);
            } else if (source == ProtocolType.FILE_SYSTEM && (target == ProtocolType.HTTP_REST || target == ProtocolType.JMS_QUEUE)) {
                transformed = csvToJson(inputPayload);
            } else if (source == ProtocolType.HTTP_REST && target == ProtocolType.JMS_QUEUE) {
                transformed = enrichJsonForJms(inputPayload, context);
            } else if (source == ProtocolType.JMS_QUEUE && target == ProtocolType.HTTP_REST) {
                transformed = extractJmsJsonPayload(inputPayload);
            } else {
                // Default passthrough or JSON normalization
                transformed = inputPayload;
            }

            context.setTransformedPayload(transformed);
            context.setRawOutboundPayload(transformed);

        } catch (Exception e) {
            throw new ProtocolBridgeException(
                "Payload transformation failed from " + source + " to " + target + ": " + e.getMessage(),
                "PAYLOAD_TRANSFORMATION_STAGE", 400, e);
        }
    }

    private String jsonToSoapEnvelope(String json, String correlationId) throws Exception {
        JsonNode rootNode = objectMapper.readTree(json);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        xml.append("xmlns:ord=\"http://hexaware.com/osb/order\">\n");
        xml.append("  <soapenv:Header>\n");
        xml.append("    <ord:CorrelationId>").append(correlationId).append("</ord:CorrelationId>\n");
        xml.append("  </soapenv:Header>\n");
        xml.append("  <soapenv:Body>\n");
        xml.append("    <ord:ProcessOrderRequest>\n");

        if (rootNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                xml.append("      <ord:").append(entry.getKey()).append(">")
                   .append(entry.getValue().asText())
                   .append("</ord:").append(entry.getKey()).append(">\n");
            }
        }

        xml.append("    </ord:ProcessOrderRequest>\n");
        xml.append("  </soapenv:Body>\n");
        xml.append("</soapenv:Envelope>");

        return xml.toString();
    }

    private String soapEnvelopeToJson(String soapXml) {
        ObjectNode json = objectMapper.createObjectNode();
        try {
            if (soapXml.contains("<ord:orderId>")) {
                json.put("orderId", extractTagContent(soapXml, "ord:orderId"));
            } else if (soapXml.contains("<orderId>")) {
                json.put("orderId", extractTagContent(soapXml, "orderId"));
            }
            if (soapXml.contains("<ord:customer>")) {
                json.put("customer", extractTagContent(soapXml, "ord:customer"));
            }
            if (soapXml.contains("<ord:status>")) {
                json.put("status", extractTagContent(soapXml, "ord:status"));
            }
            if (soapXml.contains("<ord:amount>")) {
                json.put("amount", Double.parseDouble(extractTagContent(soapXml, "ord:amount")));
            }
            json.put("bridgeStatus", "PROCESSED_FROM_SOAP");
        } catch (Exception e) {
            json.put("rawXmlSnippet", soapXml.substring(0, Math.min(soapXml.length(), 200)));
            json.put("bridgeStatus", "SOAP_PARSE_FALLBACK");
        }
        return json.toPrettyString();
    }

    private String csvToJson(String csv) throws Exception {
        String[] lines = csv.split("\n");
        if (lines.length == 0) return "[]";

        String[] headers = lines[0].split(",");
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("batchId", "BATCH-" + System.currentTimeMillis());
        rootNode.put("totalRecords", lines.length - 1);

        com.fasterxml.jackson.databind.node.ArrayNode records = rootNode.putArray("records");

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) continue;
            String[] values = lines[i].split(",");
            ObjectNode record = objectMapper.createObjectNode();
            for (int j = 0; j < headers.length; j++) {
                String val = j < values.length ? values[j].trim() : "";
                record.put(headers[j].trim(), val);
            }
            records.add(record);
        }

        return rootNode.toPrettyString();
    }

    private String enrichJsonForJms(String json, CanonicalMessage context) throws Exception {
        JsonNode rootNode = objectMapper.readTree(json);
        ObjectNode enrichedNode;
        if (rootNode.isObject()) {
            enrichedNode = (ObjectNode) rootNode.deepCopy();
        } else {
            enrichedNode = objectMapper.createObjectNode();
            enrichedNode.set("originalData", rootNode);
        }

        // Inject OSB Protocol Bridge Metadata
        ObjectNode metadata = enrichedNode.putObject("_osbProtocolBridge");
        metadata.put("sourceProtocol", context.getSourceProtocol().name());
        metadata.put("targetProtocol", context.getTargetProtocol().name());
        metadata.put("correlationId", context.getCorrelationId());
        metadata.put("messageId", context.getMessageId());
        metadata.put("bridgedAt", System.currentTimeMillis());

        return enrichedNode.toPrettyString();
    }

    private String extractJmsJsonPayload(String jmsPayload) {
        return jmsPayload; // Return raw or formatted JSON
    }

    private String extractTagContent(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int start = xml.indexOf(startTag);
        int end = xml.indexOf(endTag);
        if (start != -1 && end != -1) {
            return xml.substring(start + startTag.length(), end).trim();
        }
        return "";
    }
}
