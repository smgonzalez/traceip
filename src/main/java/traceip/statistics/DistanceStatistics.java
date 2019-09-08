package traceip.statistics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DistanceStatistics {

    private Long max = 0L;
    private Long min = 0L;
    private Long average = 0L;

    @JsonCreator
    public DistanceStatistics(
            @JsonProperty("max") Long max,
            @JsonProperty("min") Long min,
            @JsonProperty("average") Long average
    ) {
        this.max = max;
        this.min = min;
        this.average = average;
    }

    public Long getMax() {
        return max;
    }

    public Long getMin() {
        return min;
    }

    public Long getAverage() {
        return average;
    }
}
