package com.planagent.model;

import java.util.*;

public class SessionContext {
    public enum Scenario { FAMILY, FRIENDS, UNKNOWN }

    public String sessionId;
    public Scenario scenario = Scenario.UNKNOWN;
    public int partySize;
    public List<String> preferences = new ArrayList<>();
    public List<Activity> candidateActivities = new ArrayList<>();
    public List<Restaurant> candidateRestaurants = new ArrayList<>();
    public String confirmedPlan;
    public Map<String, OrderResult> executedOrders = new LinkedHashMap<>();
    public Map<String, Reservation> reservations = new LinkedHashMap<>();
    public String startTime = "14:00";

    public SessionContext(String sessionId) {
        this.sessionId = sessionId;
    }
}
