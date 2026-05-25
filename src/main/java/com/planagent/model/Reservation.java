package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Reservation {
    @JsonProperty("reservationId") public String reservationId;
    @JsonProperty("restaurantName") public String restaurantName;
    @JsonProperty("timeSlot") public String timeSlot;
    @JsonProperty("partySize") public int partySize;
    @JsonProperty("queueNumber") public String queueNumber;
    @JsonProperty("status") public String status;

    public Reservation() {}
    public Reservation(String reservationId, String restaurantName, String timeSlot,
                       int partySize, String queueNumber, String status) {
        this.reservationId = reservationId; this.restaurantName = restaurantName;
        this.timeSlot = timeSlot; this.partySize = partySize;
        this.queueNumber = queueNumber; this.status = status;
    }
}
