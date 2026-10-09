package com.hexaware.osb.protocolbridge;

import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.pipeline.MessagePipelineProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RestToSoapBridgeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MessagePipelineProcessor pipelineProcessor;

    @Test
    @DisplayName("Should successfully bridge HTTP REST (JSON) -> SOAP XML Envelope -> Mock SOAP Backend -> JSON Response")
    void testRestToSoapProtocolBridge() {
        CanonicalMessage context = new CanonicalMessage();
        context.setSourceProtocol(ProtocolType.HTTP_REST);
        context.setTargetProtocol(ProtocolType.HTTP_SOAP);
        context.setRawInboundPayload("{\n" +
                "  \"orderId\": \"ORD-SOAP-5002\",\n" +
                "  \"customer\": \"Fintech Inc\",\n" +
                "  \"amount\": 1500.00\n" +
                "}");
        context.setSourceEndpoint("/api/v1/bridge/rest-to-soap");
        context.setTargetEndpoint("http://localhost:" + port + "/mock/soap/OrderService");

        BridgeResponse response = pipelineProcessor.executePipeline(context);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(ProtocolType.HTTP_REST, response.getSourceProtocol());
        assertEquals(ProtocolType.HTTP_SOAP, response.getTargetProtocol());
        assertTrue(response.getResponsePayload().contains("ProcessOrderResponse") || response.getResponsePayload().contains("FULFILLED"));
    }
}
