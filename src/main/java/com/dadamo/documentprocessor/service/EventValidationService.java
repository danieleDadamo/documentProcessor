package com.dadamo.documentprocessor.service;

import com.dadamo.documentprocessor.event.DocumentReceivedEvent;
import com.dadamo.documentprocessor.exception.ValidationException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class EventValidationService {

    public void validate(DocumentReceivedEvent event) {
        if (event == null) {
            throw new ValidationException(List.of("event is required"));
        }

        var errors = new ArrayList<String>();

        if (event.documentId() == null || event.documentId().isBlank()) {
            errors.add("documentId must be present and not empty");
        }

        if (event.storageRef() == null || event.storageRef().isBlank()) {
            errors.add("storageRef must be present and not empty");
        } else if (!Files.isRegularFile(Paths.get(event.storageRef()))) {
            errors.add("storageRef must point to an existing file: " + event.storageRef());
        }

        if (event.metadata() == null) {
            errors.add("metadata is required");
        } else {
            if (event.metadata().type() == null) {
                errors.add("metadata.type is required");
            }
            if (event.metadata().receivedAt() == null) {
                errors.add("metadata.receivedAt is required");
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
