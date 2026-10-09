package com.hexaware.osb.protocolbridge;

import com.hexaware.osb.protocolbridge.adapter.inbound.FileInboundWatcher;
import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileToRestBridgeTest {

    @Autowired
    private FileInboundWatcher fileInboundWatcher;

    @Test
    @DisplayName("Should successfully transform CSV File payload into JSON and bridge to target protocol")
    void testFileToJmsBridge() {
        String csvData = "orderId,customer,amount\nORD-CSV-101,Acme Corp,450.00\nORD-CSV-102,Globex,1250.00";
        
        BridgeResponse response = fileInboundWatcher.processManualFileContent("bulk_orders.csv", csvData, ProtocolType.JMS_QUEUE);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(ProtocolType.FILE_SYSTEM, response.getSourceProtocol());
        assertEquals(ProtocolType.JMS_QUEUE, response.getTargetProtocol());
    }
}
