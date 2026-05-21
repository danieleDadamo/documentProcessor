package com.dadamo.documentprocessor.event;

import com.dadamo.documentprocessor.model.ReceivedMetadata;

public record DocumentReceivedEvent(String documentId, String storageRef, ReceivedMetadata metadata) {
}
