package com.hexaware.osb.protocolbridge.ui;

import com.hexaware.osb.protocolbridge.adapter.inbound.FileInboundWatcher;
import com.hexaware.osb.protocolbridge.config.ActiveMQJmsConfig;
import com.hexaware.osb.protocolbridge.domain.AuditLogEntry;
import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final AuditLogService auditLogService;
    private final FileInboundWatcher fileInboundWatcher;
    private final JmsTemplate jmsTemplate;

    public DashboardController(AuditLogService auditLogService,
                               FileInboundWatcher fileInboundWatcher,
                               JmsTemplate jmsTemplate) {
        this.auditLogService = auditLogService;
        this.fileInboundWatcher = fileInboundWatcher;
        this.jmsTemplate = jmsTemplate;
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLogEntry>> getAuditLogs() {
        return ResponseEntity.ok(auditLogService.getAuditLogs());
    }

    @DeleteMapping("/logs")
    public ResponseEntity<Map<String, String>> clearLogs() {
        auditLogService.clearLogs();
        return ResponseEntity.ok(Map.of("message", "Audit logs cleared successfully"));
    }

    @PostMapping("/simulate-jms-inbound")
    public ResponseEntity<Map<String, String>> simulateJmsInbound(@RequestBody String payload) {
        String corrId = "JMS-SIM-" + System.currentTimeMillis();
        jmsTemplate.send(ActiveMQJmsConfig.INBOUND_QUEUE, session -> {
            var msg = session.createTextMessage(payload);
            msg.setJMSCorrelationID(corrId);
            msg.setStringProperty("JMS_SourceSystem", "Web-Simulator");
            return msg;
        });

        return ResponseEntity.ok(Map.of(
            "status", "DISPATCHED_TO_INBOUND_QUEUE",
            "queue", ActiveMQJmsConfig.INBOUND_QUEUE,
            "correlationId", corrId
        ));
    }

    @PostMapping("/simulate-file-inbound")
    public ResponseEntity<BridgeResponse> simulateFileInbound(@RequestParam(defaultValue = "orders_sample.csv") String fileName,
                                                              @RequestBody String fileContent,
                                                              @RequestParam(defaultValue = "JMS_QUEUE") ProtocolType targetProtocol) {
        BridgeResponse response = fileInboundWatcher.processManualFileContent(fileName, fileContent, targetProtocol);
        return ResponseEntity.ok(response);
    }
}
