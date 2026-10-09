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
public class RestBusinessServiceAdapter implements BusinessServiceAdapter {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ProtocolType getSupportedProtocol() {
        return ProtocolType.HTTP_REST;
    }

    @Override
    public void invokeBusinessService(CanonicalMessage context) {
        String endpoint = context.getTargetEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            endpoint = "http://localhost:8080/mock/rest/orders";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-Correlation-ID", context.getCorrelationId());
            context.getHeaders().forEach((k, v) -> {
                if (!k.equalsIgnoreCase("Content-Length")) {
                    headers.add(k, v);
                }
            });

            HttpEntity<String> entity = new HttpEntity<>(context.getTransformedPayload(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);

            context.setResponsePayload(response.getBody());
            context.setStatusCode(response.getStatusCode().value());
            context.setStatusMessage("REST_HTTP_OK");

        } catch (Exception e) {
            throw new ProtocolBridgeException(
                "REST Business Service invocation failed to [" + endpoint + "]: " + e.getMessage(),
                "REST_BUSINESS_SERVICE_ADAPTER", 502, e);
        }
    }
}
