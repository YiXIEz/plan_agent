package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResult {
    @JsonProperty("paymentId") public String paymentId;
    @JsonProperty("orderId") public String orderId;
    @JsonProperty("amount") public String amount;
    @JsonProperty("status") public String status;

    public PaymentResult() {}
    public PaymentResult(String paymentId, String orderId, String amount, String status) {
        this.paymentId = paymentId; this.orderId = orderId;
        this.amount = amount; this.status = status;
    }
}
