package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RouteInfo {
    @JsonProperty("from") public String from;
    @JsonProperty("to") public String to;
    @JsonProperty("mode") public String mode;
    @JsonProperty("distance") public String distance;
    @JsonProperty("duration") public String duration;

    public RouteInfo() {}
    public RouteInfo(String from, String to, String mode, String distance, String duration) {
        this.from = from; this.to = to; this.mode = mode;
        this.distance = distance; this.duration = duration;
    }
}
