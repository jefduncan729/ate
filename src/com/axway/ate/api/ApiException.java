package com.axway.ate.api;

import com.google.gson.JsonArray;

public class ApiException extends RuntimeException {
	private static final long serialVersionUID = 945049632896164803L;

//	private Collection<ApiError> errors;
	private JsonArray errors;
	private int statusCode;

	public ApiException() {
		super();
		errors = null;
		statusCode = 0;
		
	}

	public ApiException(Throwable t) {
		super(t);	
		errors = null;
		statusCode = 0;
	}

	public ApiException(int statusCode) {
		this();
		this.statusCode = statusCode;
	}
	
	public ApiException(JsonArray errors) {
		this();
		this.errors = errors;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiException [errors=");
		builder.append(errors);
		builder.append(", statusCode=");
		builder.append(statusCode);
		builder.append("]");
		return builder.toString();
	}

	public int getStatusCode() {
		return statusCode;
	}
}
