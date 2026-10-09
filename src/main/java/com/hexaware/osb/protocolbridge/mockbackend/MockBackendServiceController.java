package com.hexaware.osb.protocolbridge.mockbackend;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mock Downstream Services simulating legacy SOAP Web Services and REST Backends.
 */
@RestController
public class MockBackendServiceController {

    @PostMapping(value = "/mock/soap/OrderService", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
    public ResponseEntity<String> processMockSoapRequest(@RequestBody String soapXml,
                                                         @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        String soapResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ord=\"http://hexaware.com/osb/order\">\n" +
                "  <soapenv:Header>\n" +
                "    <ord:CorrelationId>" + (correlationId != null ? correlationId : "CORR-MOCK") + "</ord:CorrelationId>\n" +
                "  </soapenv:Header>\n" +
                "  <soapenv:Body>\n" +
                "    <ord:ProcessOrderResponse>\n" +
                "      <ord:orderId>ORD-998811</ord:orderId>\n" +
                "      <ord:status>FULFILLED</ord:status>\n" +
                "      <ord:customer>Acme Global</ord:customer>\n" +
                "      <ord:amount>1250.75</ord:amount>\n" +
                "    </ord:ProcessOrderResponse>\n" +
                "  </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        return ResponseEntity.ok(soapResponse);
    }

    @PostMapping(value = "/mock/rest/orders", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> processMockRestRequest(@RequestBody Map<String, Object> body,
                                                                      @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId) {
        return ResponseEntity.ok(Map.of(
            "orderId", body.getOrDefault("orderId", "ORD-REST-5544"),
            "status", "PROCESSED_SUCCESSFULLY",
            "backendSystem", "Legacy-REST-Warehouse",
            "correlationId", correlationId != null ? correlationId : "UNKNOWN",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
