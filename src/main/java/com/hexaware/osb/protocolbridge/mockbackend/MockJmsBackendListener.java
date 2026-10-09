package com.hexaware.osb.protocolbridge.mockbackend;

import com.hexaware.osb.protocolbridge.config.ActiveMQJmsConfig;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class MockJmsBackendListener {

    private static final Logger log = LoggerFactory.getLogger(MockJmsBackendListener.class);
    private final JmsTemplate jmsTemplate;

    public MockJmsBackendListener(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @JmsListener(destination = ActiveMQJmsConfig.BACKEND_QUEUE)
    public void onBackendMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                String correlationId = message.getJMSCorrelationID();
                Destination replyTo = message.getJMSReplyTo();
                String body = textMessage.getText();

                log.info("Mock JMS Backend received message. CorrelationID: {}, Body snippet: {}", 
                    correlationId, body.substring(0, Math.min(body.length(), 100)));

                if (replyTo != null) {
                    String replyJson = "{\n" +
                            "  \"jmsStatus\": \"SUCCESS\",\n" +
                            "  \"processedBy\": \"Mock-JMS-Inventory-Consumer\",\n" +
                            "  \"correlationId\": \"" + correlationId + "\",\n" +
                            "  \"ackTimestamp\": " + System.currentTimeMillis() + "\n" +
                            "}";

                    jmsTemplate.send(replyTo, session -> {
                        TextMessage replyMsg = session.createTextMessage(replyJson);
                        replyMsg.setJMSCorrelationID(correlationId);
                        return replyMsg;
                    });
                    log.info("Mock JMS Backend replied to correlation ID: {}", correlationId);
                }
            }
        } catch (JMSException e) {
            log.error("Mock JMS Backend error processing message: {}", e.getMessage(), e);
        }
    }
}
