package org.example.biodwh2starter.model;

import java.util.Objects;

public final class BioEntity {
    private final String identifier;
    private final String name;
    private final String type;

    public BioEntity(final String identifier, final String name, final String type) {
        this.identifier = requireText(identifier, "identifier");
        this.name = requireText(name, "name");
        this.type = requireText(type, "type");
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    private static String requireText(final String value, final String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    @Override
    public String toString() {
        return identifier + " [" + type + "] " + name;
    }
}
