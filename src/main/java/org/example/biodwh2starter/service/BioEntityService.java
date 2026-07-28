package org.example.biodwh2starter.service;

import org.example.biodwh2starter.model.BioEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BioEntityService {
    private final List<BioEntity> entities = new ArrayList<>();

    public void add(final BioEntity entity) {
        entities.add(Objects.requireNonNull(entity, "entity must not be null"));
    }

    public List<BioEntity> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(entities));
    }

    public Optional<BioEntity> findByIdentifier(final String identifier) {
        Objects.requireNonNull(identifier, "identifier must not be null");
        return entities.stream()
                       .filter(entity -> entity.getIdentifier().equals(identifier))
                       .findFirst();
    }
}
