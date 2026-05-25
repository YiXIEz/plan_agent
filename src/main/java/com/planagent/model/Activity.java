package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {
    @JsonProperty("id") public String id;
    @JsonProperty("name") public String name;
    @JsonProperty("type") public String type;
    @JsonProperty("rating") public double rating;
    @JsonProperty("distance") public String distance;
    @JsonProperty("duration") public String duration;
    @JsonProperty("price") public String price;
    @JsonProperty("highlights") public java.util.List<String> highlights;
    @JsonProperty("ageSuitable") public String ageSuitable;
    @JsonProperty("openTime") public String openTime;

    public Activity() {}
    public Activity(String id, String name, String type, double rating, String distance,
                    String duration, String price, java.util.List<String> highlights,
                    String ageSuitable, String openTime) {
        this.id = id; this.name = name; this.type = type; this.rating = rating;
        this.distance = distance; this.duration = duration; this.price = price;
        this.highlights = highlights; this.ageSuitable = ageSuitable; this.openTime = openTime;
    }
}
