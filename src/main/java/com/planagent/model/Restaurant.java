package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Restaurant {
    @JsonProperty("id") public String id;
    @JsonProperty("name") public String name;
    @JsonProperty("cuisine") public String cuisine;
    @JsonProperty("rating") public double rating;
    @JsonProperty("distance") public String distance;
    @JsonProperty("avgPrice") public String avgPrice;
    @JsonProperty("tags") public java.util.List<String> tags;
    @JsonProperty("hasKidsMenu") public boolean hasKidsMenu;
    @JsonProperty("hasLowCalOption") public boolean hasLowCalOption;
    @JsonProperty("queueTime") public String queueTime;
    @JsonProperty("capacity") public String capacity;

    public Restaurant() {}
    public Restaurant(String id, String name, String cuisine, double rating, String distance,
                      String avgPrice, java.util.List<String> tags, boolean hasKidsMenu,
                      boolean hasLowCalOption, String queueTime, String capacity) {
        this.id = id; this.name = name; this.cuisine = cuisine; this.rating = rating;
        this.distance = distance; this.avgPrice = avgPrice; this.tags = tags;
        this.hasKidsMenu = hasKidsMenu; this.hasLowCalOption = hasLowCalOption;
        this.queueTime = queueTime; this.capacity = capacity;
    }
}
