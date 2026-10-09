package com.hexaware.osb.protocolbridge.adapter.outbound;

import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.exception.ProtocolBridgeException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileBusinessServiceAdapter implements BusinessServiceAdapter {

    private final String outputDirectory = "./outbound-files";

    @Override
    public ProtocolType getSupportedProtocol() {
        return ProtocolType.FILE_SYSTEM;
    }

    @Override
    public void invokeBusinessService(CanonicalMessage context) {
        try {
            Path outPath = Paths.get(outputDirectory);
            if (!Files.exists(outPath)) {
                Files.createDirectories(outPath);
            }

            String filename = context.getHeaders().getOrDefault("TargetFileName", "output_" + context.getCorrelationId() + ".json");
            File file = new File(outPath.toFile(), filename);

            Files.writeString(file.toPath(), context.getTransformedPayload());

            context.setResponsePayload("{\"status\":\"FILE_WRITTEN\", \"path\":\"" + file.getAbsolutePath().replace("\\", "/") + "\"}");
            context.setStatusCode(200);
            context.setStatusMessage("FILE_ADAPTER_SUCCESS");

        } catch (Exception e) {
            throw new ProtocolBridgeException(
                "File Business Service write failure: " + e.getMessage(),
                "FILE_BUSINESS_SERVICE_ADAPTER", 500, e);
        }
    }
}
