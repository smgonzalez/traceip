package traceip.tracer.country;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Currency {

    private final String code;
    private final String name;
    private final String symbol;

    @JsonCreator
    public Currency(
            @JsonProperty("code") String code,
            @JsonProperty("name") String name,
            @JsonProperty("symbol") String symbol
    ) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }
}
