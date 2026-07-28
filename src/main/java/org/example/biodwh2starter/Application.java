package org.example.biodwh2starter;

import org.example.biodwh2starter.model.BioEntity;
import org.example.biodwh2starter.service.BioEntityService;

public final class Application {
    private Application() {
        // Utility class: do not instantiate.
    }

    public static void main(final String[] args) {
        final BioEntityService service = new BioEntityService();
        service.add(new BioEntity("HGNC:11998", "TP53", "gene"));
        service.add(new BioEntity("CHEBI:15377", "Water", "compound"));

        System.out.println("BioDWH2-inspired starter is running.");
        service.findAll().forEach(entity -> System.out.println(" - " + entity));
    }
}
