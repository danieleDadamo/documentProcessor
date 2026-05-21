package com.dadamo.documentprocessor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum DocumentType {
    INVOICE, CREDIT_NOTE;

    @JsonValue
    public String toValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static DocumentType from(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
