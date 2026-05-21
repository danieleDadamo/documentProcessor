package com.dadamo.documentprocessor.service;

import com.dadamo.documentprocessor.event.DocumentProcessedEvent;
import com.dadamo.documentprocessor.event.DocumentReceivedEvent;
import com.dadamo.documentprocessor.model.ProcessedMetadata;
import com.dadamo.documentprocessor.model.ReceivedMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class DocumentProcessorService {

    private final FileSystemService fileSystemService;
    private final ObjectMapper mapper;

    public DocumentProcessorService(FileSystemService fileSystemService, ObjectMapper mapper) {
        this.fileSystemService = fileSystemService;
        this.mapper = mapper;
    }

    public DocumentProcessedEvent process(DocumentReceivedEvent receivedEvent) throws IOException {
        var content = fileSystemService.read(receivedEvent.storageRef());
        var hash = sha256(content);
        var processedMetadata = new ProcessedMetadata(
                receivedEvent.metadata().type(),
                Instant.now(),
                hash,
                content.length
        );

        var zipBytes = buildZip(content, receivedEvent.metadata(), processedMetadata, hash);
        var zipPath = fileSystemService.writeZip(receivedEvent.documentId(), zipBytes).toString();
        return new DocumentProcessedEvent(receivedEvent.documentId(), zipPath, processedMetadata);
    }

    private byte[] buildZip(
            byte[] content,
            ReceivedMetadata received,
            ProcessedMetadata processed,
            String hash
    ) throws IOException {

        var metadataJson = mapper.writeValueAsBytes(new MetadataBundle(received, processed));
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            writeEntry(zip, "invoice.pdf", content);
            writeEntry(zip, "metadata.json", metadataJson);
            writeEntry(zip, "hash.txt", hash.getBytes(StandardCharsets.UTF_8));
        }
        return out.toByteArray();
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record MetadataBundle(ReceivedMetadata received, ProcessedMetadata processed) {
    }
}
