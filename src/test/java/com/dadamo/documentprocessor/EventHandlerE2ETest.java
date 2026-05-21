package com.dadamo.documentprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class EventHandlerE2ETest {

    @Test
    void processesValidEventEndToEnd(@TempDir Path tmp) throws Exception {
        var pdfContent = "fake pdf bytes".getBytes();
        var pdfFileName = "invoice.pdf";
        var pdf = tmp.resolve(pdfFileName);
        Files.write(pdf, pdfContent);

        var receivedEventFile = tmp.resolve("event.json");
        var expectedDocumentId = "doc-1";
        var expectedDocumentType = "invoice";
        var expectedReceivedAt = "2026-05-01T10:00:00Z";
        Files.writeString(
                receivedEventFile, """
                        {
                          "documentId": "%s",
                          "storageRef": "%s",
                          "metadata": {
                            "type": "%s",
                            "receivedAt": "%s"
                          }
                        }
                        """.formatted(
                        expectedDocumentId,
                        pdf.toString().replace("\\", "\\\\"),
                        expectedDocumentType,
                        expectedReceivedAt
                )
        );

        var outputDir = tmp.resolve("output");
        var actualExitCode = runApplication(outputDir, receivedEventFile);

        assertThat(actualExitCode).isEqualTo(0);

        var outputZip = outputDir.resolve(expectedDocumentId + ".zip");
        assertThat(outputZip).exists();

        var entries = readZipEntries(Files.readAllBytes(outputZip));
        var hashFileName = "hash.txt";
        assertThat(entries).containsOnlyKeys(pdfFileName, "metadata.json", hashFileName);
        assertThat(entries.get(pdfFileName)).isEqualTo(pdfContent);
        assertThat(new String(entries.get(hashFileName))).isEqualTo(sha256Hex(pdfContent));

        var mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var metadata = mapper.readTree(entries.get("metadata.json"));

        assertThat(metadata.get("received").get("type").asText()).isEqualTo(expectedDocumentType);
        assertThat(metadata.get("received").get("receivedAt").asText()).isEqualTo(expectedReceivedAt);
        assertThat(metadata.get("processed").get("type").asText()).isEqualTo(expectedDocumentType);
        assertThat(metadata.get("processed").get("hash").asText()).isEqualTo(sha256Hex(pdfContent));
        assertThat(metadata.get("processed").get("sizeBytes").asLong()).isEqualTo(pdfContent.length);
        assertThat(metadata.get("processed").get("processedAt").asText()).isNotBlank();
    }

    @Test
    void exitsWithValidationCodeForBadEvent(@TempDir Path tmp) throws Exception {
        var eventFile = tmp.resolve("event.json");
        Files.writeString(eventFile, """
                {
                  "documentId": "",
                  "storageRef": "does/not/exist.pdf",
                  "metadata": { "type": "invoice", "receivedAt": "2026-05-01T10:00:00Z" }
                }
                """);

        var exitCode = runApplication(tmp.resolve("output"), eventFile);
        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    void exitsWithUsageCodeWhenNoArgs() {
        assertThat(EventHandler.run(new String[]{})).isEqualTo(42);
    }

    private static int runApplication(Path outputDir, Path eventFile) {
        var previous = System.setProperty("output.dir", outputDir.toString());
        try {
            return EventHandler.run(new String[]{eventFile.toString()});
        } finally {
            if (previous == null) {
                System.clearProperty("output.dir");
            } else {
                System.setProperty("output.dir", previous);
            }
        }
    }

    private static Map<String, byte[]> readZipEntries(byte[] zipBytes) throws Exception {
        var entriesMap = new HashMap<String, byte[]>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entriesMap.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entriesMap;
    }

    private static String sha256Hex(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }
}
