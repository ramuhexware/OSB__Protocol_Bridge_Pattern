package com.hexaware.osb.protocolbridge;

import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.pipeline.MessagePipelineProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpToJmsBridgeTest {

    @Autowired
    private MessagePipelineProcessor pipelineProcessor;

    @Test
    @DisplayName("Should successfully bridge HTTP REST (JSON) to JMS Queue with correlation reply")
    void testRestToJmsProtocolBridge() {
        CanonicalMessage context = new CanonicalMessage();
        context.setSourceProtocol(ProtocolType.HTTP_REST);
        context.setTargetProtocol(ProtocolType.JMS_QUEUE);
        context.setRawInboundPayload("{\n" +
                "  \"orderId\": \"ORD-JMS-1001\",\n" +
                "  \"customer\": \"Tech Corp\",\n" +
                "  \"amount\": 899.99\n" +
                "}");
        context.setSourceEndpoint("/api/v1/bridge/rest-to-jms");
        context.setTargetEndpoint("osb.backend.queue");
        context.addHeader("X-Source-App", "JUnit-TestRunner");

        BridgeResponse response = pipelineProcessor.executePipeline(context);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(ProtocolType.HTTP_REST, response.getSourceProtocol());
        assertEquals(ProtocolType.JMS_QUEUE, response.getTargetProtocol());
        assertNotNull(response.getCorrelationId());
        assertTrue(response.getResponsePayload().contains("SUCCESS") || response.getResponsePayload().contains("DISPATCHED"));
        assertFalse(response.getPipelineTrace().isEmpty());
    }
}
