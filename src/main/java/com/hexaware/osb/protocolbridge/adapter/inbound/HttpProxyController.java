package com.hexaware.osb.protocolbridge.adapter.inbound;

import com.hexaware.osb.protocolbridge.domain.BridgeRequest;
import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.pipeline.MessagePipelineProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/bridge")
@Tag(name = "OSB Protocol Bridge API", description = "Endpoints for executing Protocol Bridge Patterns across REST, SOAP, JMS, and File System")
public class HttpProxyController {

    private final MessagePipelineProcessor pipelineProcessor;

    public HttpProxyController(MessagePipelineProcessor pipelineProcessor) {
        this.pipelineProcessor = pipelineProcessor;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute Generic Protocol Bridge Transformation", 
               description = "Bridges any source protocol to any target protocol (e.g. REST to JMS, REST to SOAP, JMS to REST)")
    public ResponseEntity<BridgeResponse> executeBridge(@RequestBody BridgeRequest request,
                                                        @RequestHeader Map<String, String> headers) {
        CanonicalMessage context = new CanonicalMessage();
        context.setSourceProtocol(request.getSourceProtocol() != null ? request.getSourceProtocol() : ProtocolType.HTTP_REST);
        context.setTargetProtocol(request.getTargetProtocol() != null ? request.getTargetProtocol() : ProtocolType.JMS_QUEUE);
        context.setRawInboundPayload(request.getPayload());
        context.setSourceEndpoint("/api/v1/bridge/execute");
        context.setTargetEndpoint(request.getTargetEndpoint());
        context.setHeaders(headers);

        if (request.getHeaders() != null) {
            context.getHeaders().putAll(request.getHeaders());
        }

        BridgeResponse response = pipelineProcessor.executePipeline(context);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/rest-to-jms")
    @Operation(summary = "Bridge HTTP REST (JSON) -> JMS Queue", 
               description = "Inbound REST Proxy converts JSON payload and HTTP headers into JMS TextMessage and dispatches to JMS queue")
    public ResponseEntity<BridgeResponse> bridgeRestToJms(@RequestBody String jsonPayload,
                                                          @RequestHeader Map<String, String> headers) {
        CanonicalMessage context = new CanonicalMessage();
        context.setSourceProtocol(ProtocolType.HTTP_REST);
        context.setTargetProtocol(ProtocolType.JMS_QUEUE);
        context.setRawInboundPayload(jsonPayload);
        context.setSourceEndpoint("/api/v1/bridge/rest-to-jms");
        context.setTargetEndpoint("osb.backend.queue");
        context.setHeaders(headers);

        BridgeResponse response = pipelineProcessor.executePipeline(context);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/rest-to-soap")
    @Operation(summary = "Bridge HTTP REST (JSON) -> HTTP SOAP Web Service (XML)", 
               description = "Inbound REST Proxy transforms JSON to SOAP XML Envelope, calls legacy SOAP Web Service, and transforms SOAP response XML back to JSON")
    public ResponseEntity<BridgeResponse> bridgeRestToSoap(@RequestBody String jsonPayload,
                                                           @RequestHeader Map<String, String> headers) {
        CanonicalMessage context = new CanonicalMessage();
        context.setSourceProtocol(ProtocolType.HTTP_REST);
        context.setTargetProtocol(ProtocolType.HTTP_SOAP);
        context.setRawInboundPayload(jsonPayload);
        context.setSourceEndpoint("/api/v1/bridge/rest-to-soap");
        context.setTargetEndpoint("http://localhost:8080/mock/soap/OrderService");
        context.setHeaders(headers);

        BridgeResponse response = pipelineProcessor.executePipeline(context);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
