package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueInfo {
    @JsonProperty("restaurantName") public String restaurantName;
    @JsonProperty("queueSize") public int queueSize;
    @JsonProperty("estimatedWaitMinutes") public int estimatedWaitMinutes;
    @JsonProperty("available") public boolean available;

    public QueueInfo() {}
    public QueueInfo(String restaurantName, int queueSize, int estimatedWaitMinutes, boolean available) {
        this.restaurantName = restaurantName; this.queueSize = queueSize;
        this.estimatedWaitMinutes = estimatedWaitMinutes; this.available = available;
    }
}
