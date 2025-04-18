package com.nokia.dos.md.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AsyncResponse {

	
	@NotNull
	private String status;
	@JsonProperty("NSSIId")
    private Integer nssiId;
    private Integer sliceProfileId;
}
