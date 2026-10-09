package com.hexaware.osb.protocolbridge.pipeline;

import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.exception.ProtocolBridgeException;
import com.hexaware.osb.protocolbridge.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessagePipelineProcessor {

    private static final Logger log = LoggerFactory.getLogger(MessagePipelineProcessor.class);

    private final HeaderTransformStage headerTransformStage;
    private final PayloadTransformStage payloadTransformStage;
    private final ProtocolBridgeRouter protocolBridgeRouter;
    private final AuditLogService auditLogService;

    public MessagePipelineProcessor(HeaderTransformStage headerTransformStage,
                                    PayloadTransformStage payloadTransformStage,
                                    ProtocolBridgeRouter protocolBridgeRouter,
                                    AuditLogService auditLogService) {
        this.headerTransformStage = headerTransformStage;
        this.payloadTransformStage = payloadTransformStage;
        this.protocolBridgeRouter = protocolBridgeRouter;
        this.auditLogService = auditLogService;
    }

    public BridgeResponse executePipeline(CanonicalMessage context) {
        long startTime = System.currentTimeMillis();
        BridgeResponse response = new BridgeResponse();
        response.setMessageId(context.getMessageId());
        response.setCorrelationId(context.getCorrelationId());
        response.setSourceProtocol(context.getSourceProtocol());
        response.setTargetProtocol(context.getTargetProtocol());

        try {
            // Stage 1: Inbound Pipeline Entry
            response.addPipelineTraceStep("STAGE 1 [INBOUND_PROXY]: Received request over " 
                + context.getSourceProtocol() + " from [" + context.getSourceEndpoint() + "]");
            log.info("OSB Pipeline Starting MSG ID: {}, Source: {}, Target: {}", 
                context.getMessageId(), context.getSourceProtocol(), context.getTargetProtocol());

            // Stage 2: Header Transformation Stage
            headerTransformStage.processHeaderTranslation(context);
            response.addPipelineTraceStep("STAGE 2 [HEADER_TRANSFORM]: Translated " 
                + context.getHeaders().size() + " metadata headers for target protocol " + context.getTargetProtocol());

            // Stage 3: Payload Transformation Stage
            payloadTransformStage.processPayloadTransformation(context);
            response.addPipelineTraceStep("STAGE 3 [PAYLOAD_TRANSFORM]: Transformed payload from " 
                + context.getSourceProtocol().getDefaultContentType() + " -> " + context.getTargetProtocol().getDefaultContentType());

            // Stage 4: Routing & Business Service Adapter Stage
            response.addPipelineTraceStep("STAGE 4 [PROTOCOL_ROUTER]: Routing request to Business Service Adapter for " 
                + context.getTargetProtocol() + " -> [" + context.getTargetEndpoint() + "]");
            protocolBridgeRouter.routeToBusinessService(context);

            // Stage 5: Outbound Response Mapping Stage
            response.setStatusCode(context.getStatusCode());
            response.setStatusMessage(context.getStatusMessage());
            response.setResponsePayload(context.getResponsePayload());
            response.setResponseHeaders(context.getHeaders());
            response.addPipelineTraceStep("STAGE 5 [OUTBOUND_RESPONSE]: Received status " 
                + context.getStatusCode() + " (" + context.getStatusMessage() + ") from target backend service");

        } catch (ProtocolBridgeException e) {
            log.error("OSB Pipeline Fault in Stage [{}]: {}", e.getStage(), e.getMessage());
            response.setStatusCode(e.getStatusCode());
            response.setStatusMessage("FAULT: " + e.getStage() + " - " + e.getMessage());
            response.setResponsePayload(formatFaultPayload(e, context));
            response.addPipelineTraceStep("ERROR_STAGE [" + e.getStage() + "]: " + e.getMessage());

        } catch (Exception e) {
            log.error("OSB Unhandled Pipeline Error: {}", e.getMessage(), e);
            response.setStatusCode(500);
            response.setStatusMessage("SYSTEM_ERROR: " + e.getMessage());
            response.setResponsePayload("{\"error\":\"Internal OSB Bridge Error\", \"details\":\"" + e.getMessage() + "\"}");
            response.addPipelineTraceStep("ERROR_STAGE [UNHANDLED]: " + e.getMessage());
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            response.setExecutionTimeMs(duration);
            response.addPipelineTraceStep("PIPELINE_COMPLETE: Total Latency " + duration + " ms");
            auditLogService.recordTransaction(context, response);
        }

        return response;
    }

    private String formatFaultPayload(ProtocolBridgeException e, CanonicalMessage context) {
        return "{\n" +
               "  \"fault\": {\n" +
               "    \"code\": \"OSB-BRIDGE-" + e.getStatusCode() + "\",\n" +
               "    \"stage\": \"" + e.getStage() + "\",\n" +
               "    \"message\": \"" + e.getMessage() + "\",\n" +
               "    \"correlationId\": \"" + context.getCorrelationId() + "\"\n" +
               "  }\n" +
               "}";
    }
}
