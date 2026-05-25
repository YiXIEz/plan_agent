package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationResult {
    @JsonProperty("recipient") public String recipient;
    @JsonProperty("channel") public String channel;
    @JsonProperty("status") public String status;
    @JsonProperty("preview") public String preview;

    public NotificationResult() {}
    public NotificationResult(String recipient, String channel, String status, String preview) {
        this.recipient = recipient; this.channel = channel;
        this.status = status; this.preview = preview;
    }
}
