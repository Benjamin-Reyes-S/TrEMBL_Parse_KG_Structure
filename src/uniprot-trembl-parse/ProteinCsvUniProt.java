import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "accession",
        "names",
        "sequence"
})
public class ProteinCsvRow {

    @JsonProperty("accession")
    public String accession;

    @JsonProperty("names")
    public String names;

    @JsonProperty("sequence")
    public String sequence;

    public ProteinCsvRow() {
    }

    public ProteinCsvRow(String accession, String names, String sequence) {
        this.accession = accession;
        this.names = names;
        this.sequence = sequence;
    }
}