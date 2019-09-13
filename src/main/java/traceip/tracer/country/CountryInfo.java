package traceip.tracer.country;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryInfo {

    private final String isoCode;
    private final String name;
    private final String nativeName;
    private final List<Language> languages;
    private final List<String> timezones;
    private final List<Double> latlng;
    private final List<Currency> currencies;

    @JsonCreator
    public CountryInfo(
            @JsonProperty("alpha2Code") String isoCode,
            @JsonProperty("name") String name,
            @JsonProperty("nativeName") String nativeName,
            @JsonProperty("languages") List<Language> languages,
            @JsonProperty("timezones") List<String> timezones,
            @JsonProperty("latlng") List<Double> latlng,
            @JsonProperty("currencies") List<Currency> currencies
    ) {
        this.isoCode = isoCode;
        this.name = name;
        this.nativeName = nativeName;
        this.languages = languages;
        this.timezones = timezones;
        this.latlng = latlng;
        this.currencies = currencies;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getName() {
        return name;
    }

    public List<Language> getLanguages() {
        return languages;
    }

    public List<String> getTimezones() {
        return timezones;
    }

    public List<Double> getLatlng() {
        return latlng;
    }

    public List<Currency> getCurrencies() {
        return currencies;
    }

    public String getNativeName() {
        return nativeName;
    }
}
