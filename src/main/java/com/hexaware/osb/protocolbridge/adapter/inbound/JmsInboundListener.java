package com.hexaware.osb.protocolbridge.adapter.inbound;

import com.hexaware.osb.protocolbridge.config.ActiveMQJmsConfig;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.pipeline.MessagePipelineProcessor;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.Enumeration;

@Component
public class JmsInboundListener {

    private static final Logger log = LoggerFactory.getLogger(JmsInboundListener.class);
    private final MessagePipelineProcessor pipelineProcessor;

    public JmsInboundListener(MessagePipelineProcessor pipelineProcessor) {
        this.pipelineProcessor = pipelineProcessor;
    }

    @JmsListener(destination = ActiveMQJmsConfig.INBOUND_QUEUE)
    public void processInboundJmsMessage(Message message) {
        try {
            if (message instanceof TextMessage textMessage) {
                CanonicalMessage context = new CanonicalMessage();
                context.setSourceProtocol(ProtocolType.JMS_QUEUE);
                context.setTargetProtocol(ProtocolType.HTTP_REST);
                context.setRawInboundPayload(textMessage.getText());
                context.setSourceEndpoint(ActiveMQJmsConfig.INBOUND_QUEUE);
                context.setTargetEndpoint("http://localhost:8080/mock/rest/orders");

                if (message.getJMSCorrelationID() != null) {
                    context.setCorrelationId(message.getJMSCorrelationID());
                }

                // Copy JMS properties to Headers
                Enumeration<?> propNames = message.getPropertyNames();
                while (propNames.hasMoreElements()) {
                    String name = (String) propNames.nextElement();
                    context.addHeader("JMS_" + name, String.valueOf(message.getObjectProperty(name)));
                }

                log.info("Inbound JMS Proxy received message on queue: {}. CorrelationID: {}", 
                    ActiveMQJmsConfig.INBOUND_QUEUE, context.getCorrelationId());

                pipelineProcessor.executePipeline(context);
            }
        } catch (JMSException e) {
            log.error("Error processing inbound JMS message: {}", e.getMessage(), e);
        }
    }
}
