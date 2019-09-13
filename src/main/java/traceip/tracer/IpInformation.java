package traceip.tracer;

import com.fasterxml.jackson.annotation.JsonProperty;
import traceip.tracer.country.Currency;
import traceip.tracer.country.Language;
import traceip.tracer.distances.GeoLocation;

import java.util.List;

public class IpInformation {

    private final String isoCode;
    private final String name;
    private final String nativeName;
    private final List<Language> languages;
    private final String localTime;
    private final List<String> timezones;
    private final GeoLocation geoLocation;
    private final Long distanceToRefPoint;
    private final Currency currency;
    private final Double dolarConvertion;

    public IpInformation(
            @JsonProperty("isoCode") String isoCode,
            @JsonProperty("name") String name,
            @JsonProperty("nativeName") String nativeName,
            @JsonProperty("languages") List<Language> languages,
            @JsonProperty("localTime") String localTime,
            @JsonProperty("timezones") List<String> timezones,
            @JsonProperty("geoLocation") GeoLocation geoLocation,
            @JsonProperty("distanceToRefPoint") Long distanceToRefPoint,
            @JsonProperty("currency") Currency currency,
            @JsonProperty("dolarConvertion") Double dolarConvertion
    ) {
        this.isoCode = isoCode;
        this.name = name;
        this.nativeName = nativeName;
        this.languages = languages;
        this.localTime = localTime;
        this.timezones = timezones;
        this.geoLocation = geoLocation;
        this.distanceToRefPoint = distanceToRefPoint;
        this.currency = currency;
        this.dolarConvertion = dolarConvertion;
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

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Double getDolarConvertion() {
        return dolarConvertion;
    }

    public String getLocalTime() {
        return localTime;
    }

    public Long getDistanceToRefPoint() {
        return distanceToRefPoint;
    }

    public String getNativeName() {
        return nativeName;
    }
}
