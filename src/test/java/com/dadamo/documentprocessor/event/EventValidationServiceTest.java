package com.dadamo.documentprocessor.event;

import com.dadamo.documentprocessor.exception.ValidationException;
import com.dadamo.documentprocessor.model.DocumentType;
import com.dadamo.documentprocessor.model.ReceivedMetadata;
import com.dadamo.documentprocessor.service.EventValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventValidationServiceTest {

    private final EventValidationService sut = new EventValidationService();

    @Test
    void acceptsValidEvent(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("invoice.pdf");
        Files.writeString(file, "x");
        String documentId = "123";
        var event = new DocumentReceivedEvent(documentId, file.toString(),
                new ReceivedMetadata(DocumentType.INVOICE, Instant.parse("2026-05-01T10:00:00Z")));

        sut.validate(event);
    }

    @Test
    void rejectsNullEvent() {
        assertThatThrownBy(() -> sut.validate(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("event is required");
    }

    @Test
    void rejectsBlankDocumentId(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("a.pdf");
        Files.writeString(file, "x");
        String whiteSpaceString = "  ";
        var event = new DocumentReceivedEvent(whiteSpaceString, file.toString(),
                new ReceivedMetadata(DocumentType.INVOICE, Instant.now()));

        assertThatThrownBy(() -> sut.validate(event))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("documentId");
    }

    @Test
    void rejectsMissingStorageRef() {
        var event = new DocumentReceivedEvent("123", null,
                new ReceivedMetadata(DocumentType.INVOICE, Instant.now()));

        assertThatThrownBy(() -> sut.validate(event))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("storageRef");
    }

    @Test
    void rejectsNonExistentStorageRef() {
        var event = new DocumentReceivedEvent("123", "no/such/file.pdf",
                new ReceivedMetadata(DocumentType.INVOICE, Instant.now()));

        assertThatThrownBy(() -> sut.validate(event))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("existing file");
    }

    @Test
    void rejectsMissingMetadata(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("a.pdf");
        Files.writeString(file, "x");
        var event = new DocumentReceivedEvent("123", file.toString(), null);

        assertThatThrownBy(() -> sut.validate(event))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("metadata");
    }

    @Test
    void rejectsMissingMetadataFields(@TempDir Path tmp) throws Exception {
        var file = tmp.resolve("a.pdf");
        Files.writeString(file, "x");
        var event = new DocumentReceivedEvent("123", file.toString(),
                new ReceivedMetadata(null, null));

        assertThatThrownBy(() -> sut.validate(event))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("metadata.type")
                .hasMessageContaining("metadata.receivedAt");
    }

    @Test
    void collectsAllErrorsAtOnce() {
        var event = new DocumentReceivedEvent("", "", null);

        try {
            sut.validate(event);
        } catch (ValidationException e) {
            assertThat(e.errors()).hasSizeGreaterThanOrEqualTo(3);
            return;
        }
        throw new AssertionError("expected ValidationException");
    }
}
