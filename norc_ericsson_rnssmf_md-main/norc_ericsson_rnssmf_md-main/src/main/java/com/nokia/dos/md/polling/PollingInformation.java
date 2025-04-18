package com.nokia.dos.md.polling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
public class PollingInformation {
    
	String operation;
    String orderId;
    String status;

    public PollingInformation(
        @JsonProperty(value = "operation", required = true) String operation,
        @JsonProperty(value = "orderId", required = true) String orderId,
        @JsonProperty(value = "status", required = true) String status)
    {
        this.operation = operation;
        this.orderId = orderId;
        this.status = status;
    }
}
