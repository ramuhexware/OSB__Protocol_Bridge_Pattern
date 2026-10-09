package com.hexaware.osb.protocolbridge.adapter.inbound;

import com.hexaware.osb.protocolbridge.domain.BridgeResponse;
import com.hexaware.osb.protocolbridge.domain.CanonicalMessage;
import com.hexaware.osb.protocolbridge.domain.ProtocolType;
import com.hexaware.osb.protocolbridge.pipeline.MessagePipelineProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileInboundWatcher {

    private static final Logger log = LoggerFactory.getLogger(FileInboundWatcher.class);
    private final MessagePipelineProcessor pipelineProcessor;
    private final String inboundDirectory = "./inbound-files";

    public FileInboundWatcher(MessagePipelineProcessor pipelineProcessor) {
        this.pipelineProcessor = pipelineProcessor;
        initDirectory();
    }

    private void initDirectory() {
        try {
            Path path = Paths.get(inboundDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            log.error("Failed to initialize inbound file directory: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void scanInboundFiles() {
        File dir = new File(inboundDirectory);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".csv") || name.endsWith(".txt") || name.endsWith(".xml"));

        if (files == null || files.length == 0) return;

        for (File file : files) {
            try {
                log.info("File Inbound Adapter processing file: {}", file.getName());
                String content = Files.readString(file.toPath());

                CanonicalMessage context = new CanonicalMessage();
                context.setSourceProtocol(ProtocolType.FILE_SYSTEM);
                context.setTargetProtocol(ProtocolType.JMS_QUEUE);
                context.setRawInboundPayload(content);
                context.setSourceEndpoint(file.getAbsolutePath());
                context.setTargetEndpoint("osb.backend.queue");
                context.addHeader("FileName", file.getName());

                BridgeResponse response = pipelineProcessor.executePipeline(context);

                // Move file to processed archive
                File archiveDir = new File(dir, "archive");
                if (!archiveDir.exists()) archiveDir.mkdirs();

                File archivedFile = new File(archiveDir, "proc_" + System.currentTimeMillis() + "_" + file.getName());
                file.renameTo(archivedFile);

                log.info("File bridge completed with status: {}, archived to: {}", 
                    response.getStatusMessage(), archivedFile.getName());

            } catch (Exception e) {
                log.error("Error processing inbound file [{}]: {}", file.getName(), e.getMessage());
            }
        }
    }

    public BridgeResponse processManualFileContent(String fileName, String fileContent, ProtocolType targetProtocol) {
        CanonicalMessage context = new CanonicalMessage();
        context.setSourceProtocol(ProtocolType.FILE_SYSTEM);
        context.setTargetProtocol(targetProtocol != null ? targetProtocol : ProtocolType.JMS_QUEUE);
        context.setRawInboundPayload(fileContent);
        context.setSourceEndpoint("./inbound-files/" + fileName);
        context.setTargetEndpoint(targetProtocol == ProtocolType.HTTP_REST ? "http://localhost:8080/mock/rest/orders" : "osb.backend.queue");
        context.addHeader("FileName", fileName);

        return pipelineProcessor.executePipeline(context);
    }
}
