package com.hexaware.osb.protocolbridge.adapter.outbound;

import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.exception.ProtocolBridgeException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpSoapBusinessServiceAdapter implements BusinessServiceAdapter {

    private final RestTemplate restTemplate;

    public HttpSoapBusinessServiceAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ProtocolType getSupportedProtocol() {
        return ProtocolType.HTTP_SOAP;
    }

    @Override
    public void invokeBusinessService(CanonicalMessage context) {
        String endpoint = context.getTargetEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = "http://localhost:8080/mock/soap/OrderService";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);
            headers.add("SOAPAction", context.getHeaders().getOrDefault("SOAPAction", "processOrder"));
            headers.add("X-Correlation-ID", context.getCorrelationId());

            HttpEntity<String> entity = new HttpEntity<>(context.getTransformedPayload(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);

            context.setResponsePayload(response.getBody());
            context.setStatusCode(response.getStatusCode().value());
            context.setStatusMessage("SOAP_HTTP_RESPONSE_OK");

        } catch (Exception e) {
            throw new ProtocolBridgeException(
                "SOAP Business Service HTTP call failed to endpoint [" + endpoint + "]: " + e.getMessage(),
                "SOAP_BUSINESS_SERVICE_ADAPTER", 502, e);
        }
    }
}
