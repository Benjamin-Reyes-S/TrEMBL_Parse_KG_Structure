package org.example.biodwh2starter.uniprot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A publication cited by a UniProt entry. */
public final class Citation {
    private final String title;
    private final String date;
    private final List<String> authorList;
    private final List<String> dbReferences;

    public Citation(String title, String date, List<String> authorList,
            List<String> dbReferences) {
        this.title = Objects.requireNonNull(title, "title");
        this.date = date;
        this.authorList = Collections.unmodifiableList(new ArrayList<>(authorList));
        this.dbReferences = Collections.unmodifiableList(new ArrayList<>(dbReferences));
    }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public List<String> getAuthorList() { return authorList; }
    public List<String> getDbReferences() { return dbReferences; }

    /** Identifier used by citations.csv and the protein/citation mapping. */
    public String getTitleAndDate() {
        return date == null || date.isEmpty() ? title : title + " (" + date + ")";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Citation
                && getTitleAndDate().equals(((Citation) other).getTitleAndDate());
    }

    @Override
    public int hashCode() { return getTitleAndDate().hashCode(); }
}
