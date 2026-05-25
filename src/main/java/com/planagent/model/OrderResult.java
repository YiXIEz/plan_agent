package com.planagent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResult {
    @JsonProperty("orderId") public String orderId;
    @JsonProperty("productType") public String productType;
    @JsonProperty("productName") public String productName;
    @JsonProperty("quantity") public int quantity;
    @JsonProperty("totalPrice") public String totalPrice;
    @JsonProperty("deliveryTime") public String deliveryTime;
    @JsonProperty("status") public String status;

    public OrderResult() {}
    public OrderResult(String orderId, String productType, String productName, int quantity,
                       String totalPrice, String deliveryTime, String status) {
        this.orderId = orderId; this.productType = productType; this.productName = productName;
        this.quantity = quantity; this.totalPrice = totalPrice; this.deliveryTime = deliveryTime;
        this.status = status;
    }
}
