import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "organism")
public class Organism {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "name")
    private List<OrganismName> names = new ArrayList<>();

    @JacksonXmlProperty(localName = "dbReference")
    private DbReference dbReference;

    public Organism() {
    }

    public List<OrganismName> getNames() {
        return names;
    }

    public void setNames(List<OrganismName> names) {
        this.names = names;
    }

    public DbReference getDbReference() {
        return dbReference;
    }

    public void setDbReference(DbReference dbReference) {
        this.dbReference = dbReference;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrganismName {

        @JacksonXmlProperty(isAttribute = true, localName = "type")
        private String type;

        @JacksonXmlText
        private String value;

        public OrganismName() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DbReference {

        @JacksonXmlProperty(isAttribute = true, localName = "type")
        private String type;

        @JacksonXmlProperty(isAttribute = true, localName = "id")
        private String id;

        public DbReference() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}