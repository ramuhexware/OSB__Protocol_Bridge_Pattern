package com.hexaware.osb.protocolbridge.adapter.outbound;

import com.hexaware.osb.protocolbridge.config.ActiveMQJmsConfig;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.exception.ProtocolBridgeException;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JmsBusinessServiceAdapter implements BusinessServiceAdapter {

    private final JmsTemplate jmsTemplate;

    public JmsBusinessServiceAdapter(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public ProtocolType getSupportedProtocol() {
        return ProtocolType.JMS_QUEUE;
    }

    @Override
    public void invokeBusinessService(CanonicalMessage context) {
        String destinationQueue = context.getTargetEndpoint();
        if (destinationQueue == null || destinationQueue.trim().isEmpty()) {
            destinationQueue = ActiveMQJmsConfig.BACKEND_QUEUE;
        }

        try {
            final String targetQueue = destinationQueue;
            final String payload = context.getTransformedPayload();

            // Send JMS message with Correlation ID and headers
            jmsTemplate.send(targetQueue, session -> {
                TextMessage message = session.createTextMessage(payload);
                message.setJMSCorrelationID(context.getCorrelationId());
                message.setJMSReplyTo(session.createQueue(ActiveMQJmsConfig.BACKEND_REPLY_QUEUE));

                // Map context headers to JMS Properties
                for (Map.Entry<String, String> entry : context.getHeaders().entrySet()) {
                    message.setStringProperty(entry.getKey(), entry.getValue());
                }
                return message;
            });

            // Synchronous Request-Reply wait using Correlation ID selector
            String selector = "JMSCorrelationID = '" + context.getCorrelationId() + "'";
            Message reply = jmsTemplate.receiveSelected(ActiveMQJmsConfig.BACKEND_REPLY_QUEUE, selector);

            if (reply instanceof TextMessage textMessage) {
                context.setResponsePayload(textMessage.getText());
                context.setStatusCode(200);
                context.setStatusMessage("JMS_REPLY_RECEIVED");
            } else {
                // If async queue dispatch without waiting or timeout
                context.setResponsePayload("{\"status\":\"DISPATCHED_TO_JMS_QUEUE\", \"queue\":\"" + targetQueue + "\", \"correlationId\":\"" + context.getCorrelationId() + "\"}");
                context.setStatusCode(202);
                context.setStatusMessage("JMS_ACCEPTED");
            }

        } catch (JMSException e) {
            throw new ProtocolBridgeException(
                "JMS Business Service Adapter dispatch failure: " + e.getMessage(),
                "JMS_BUSINESS_SERVICE_ADAPTER", 500, e);
        }
    }
}
