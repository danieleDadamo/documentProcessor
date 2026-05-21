package com.dadamo.documentprocessor.model;

import java.time.Instant;

public record ReceivedMetadata(DocumentType type, Instant receivedAt) {
}
