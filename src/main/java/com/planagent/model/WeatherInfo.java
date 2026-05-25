package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherInfo {
    @JsonProperty("district") public String district;
    @JsonProperty("weather") public String weather;
    @JsonProperty("temperature") public String temperature;
    @JsonProperty("suitable") public boolean suitable;

    public WeatherInfo() {}
    public WeatherInfo(String district, String weather, String temperature, boolean suitable) {
        this.district = district; this.weather = weather;
        this.temperature = temperature; this.suitable = suitable;
    }
}
