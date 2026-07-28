import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true) //ignore fields I dont care about
public class Protein {
    @JacksonXmlProperty
    public String accession;
    @JacksonXmlProperty
    public String name;
    @JacksonXmlProperty(localName= "sequence") //because nested structure
    private ProteinSequence sequence;

    public ProteinSequence getSequence(){
        return sequence;
    }
    public void ProteinSequence{
        this.sequence= sequence;
    }

    public static class ProteinSequence{
        @JacksonXmlProperty(isAttribute=true)
        private String modified;

        @JacksonXmlProperty(isAttribute=true)
        private String version;

    }
}