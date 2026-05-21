package com.dadamo.documentprocessor.model;

import java.time.Instant;

public record ProcessedMetadata(DocumentType type, Instant processedAt, String hash, long sizeBytes) {
}
