package com.dadamo.documentprocessor.event;

import com.dadamo.documentprocessor.model.ProcessedMetadata;

public record DocumentProcessedEvent(String documentId, String zipPath, ProcessedMetadata metadata) {
}
