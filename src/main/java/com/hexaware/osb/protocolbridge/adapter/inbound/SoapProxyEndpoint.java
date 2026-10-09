package com.hexaware.osb.protocolbridge.adapter.inbound;

import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.pipeline.MessagePipelineProcessor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/proxy/soap")
public class SoapProxyEndpoint {

    private final MessagePipelineProcessor pipelineProcessor;

    public SoapProxyEndpoint(MessagePipelineProcessor pipelineProcessor) {
        this.pipelineProcessor = pipelineProcessor;
    }

    @PostMapping(value = "/OrderBridgeService", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> handleSoapProxyRequest(@RequestBody String soapXml,
                                                         @RequestHeader Map<String, String> headers) {
        CanonicalMessage context = new CanonicalMessage();
        context.setSourceProtocol(ProtocolType.HTTP_SOAP);
        context.setTargetProtocol(ProtocolType.JMS_QUEUE);
        context.setRawInboundPayload(soapXml);
        context.setSourceEndpoint("/proxy/soap/OrderBridgeService");
        context.setTargetEndpoint("osb.backend.queue");
        context.setHeaders(headers);

        BridgeResponse bridgeResponse = pipelineProcessor.executePipeline(context);

        String soapEnvelopeResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "  <soapenv:Header/>\n" +
                "  <soapenv:Body>\n" +
                "    <BridgeResult>\n" +
                "      <Status>" + bridgeResponse.getStatusMessage() + "</Status>\n" +
                "      <CorrelationId>" + bridgeResponse.getCorrelationId() + "</CorrelationId>\n" +
                "      <Payload>" + escapeXml(bridgeResponse.getResponsePayload()) + "</Payload>\n" +
                "    </BridgeResult>\n" +
                "  </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        return ResponseEntity.status(bridgeResponse.getStatusCode()).body(soapEnvelopeResponse);
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }
}
