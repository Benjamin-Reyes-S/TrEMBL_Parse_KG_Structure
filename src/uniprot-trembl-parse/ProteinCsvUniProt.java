import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonProteinPropertyOrder({
        "names",
        "accession",
        "secondary_accessions",
        "sequence",
        "sequence_modified",
        "sequence_precursor"
})

public class ProteinCsvRow {

    @JsonProperty("names")
    public String names;

    @JsonProperty("accession")
    public String accession;

    @JsonProperty("secondary_accessions")
    public String secondaryAccessions;

    @JsonProperty("sequence")
    public String sequence;

    @JsonProperty("sequence_modified")
    public String sequenceModified;

    @JsonProperty("sequence_precursor")
    public String sequencePrecursor;
}