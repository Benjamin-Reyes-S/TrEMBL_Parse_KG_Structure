package org.example.biodwh2starter.service;

import org.example.biodwh2starter.model.BioEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BioEntityServiceTest {
    @Test
    void findsEntityByIdentifier() {
        final BioEntityService service = new BioEntityService();
        service.add(new BioEntity("HGNC:11998", "TP53", "gene"));

        assertTrue(service.findByIdentifier("HGNC:11998").isPresent());
        assertEquals("TP53", service.findByIdentifier("HGNC:11998").orElseThrow().getName());
    }

    @Test
    void returnsAnImmutableSnapshot() {
        final BioEntityService service = new BioEntityService();
        service.add(new BioEntity("CHEBI:15377", "Water", "compound"));

        assertEquals(1, service.findAll().size());
        assertThrows(UnsupportedOperationException.class,
                     () -> service.findAll().add(new BioEntity("X:1", "Other", "test")));
    }
}
