package com.axway.ate.rest;

import org.springframework.http.HttpMethod;

import com.google.gson.JsonObject;

public class RestRequest {
	
	private String url;
	private JsonObject json;
	private HttpMethod method;
	
	public RestRequest() {
		super();
		url = null;
		json = null;
		method = HttpMethod.GET;
	}
	
	public RestRequest(String url, JsonObject json, HttpMethod method) {
		this();
		this.url = url;
		this.json = json;
		this.method = method;
	}
	
	public static RestRequest GETRequestFor(String url) {
		return new RestRequest(url, null, HttpMethod.GET);
	}
	
	public static RestRequest DELETERequestFor(String url, JsonObject json) {
		return new RestRequest(url, json, HttpMethod.DELETE);
	}
	
	public static RestRequest POSTRequestFor(String url, JsonObject json) {
		return new RestRequest(url, json, HttpMethod.POST);
	}
	
	public static RestRequest PUTRequestFor(String url, JsonObject json) {
		return new RestRequest(url, json, HttpMethod.PUT);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public JsonObject getJson() {
		return json;
	}

	public void setJson(JsonObject json) {
		this.json = json;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}
}
