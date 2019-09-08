package traceip.tracer.country;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Language {

    private final String isoCode;
    private final String name;

    @JsonCreator
    public Language(
            @JsonProperty("iso639_1") String isoCode,
            @JsonProperty("name") String name
    ) {
        this.isoCode = isoCode;
        this.name = name;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getName() {
        return name;
    }
}
