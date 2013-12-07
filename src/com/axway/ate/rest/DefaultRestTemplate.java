package com.axway.ate.rest;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import android.util.Log;

public class DefaultRestTemplate extends RestTemplate {

	private static RestTemplate instance;
	
	public static RestTemplate getInstance() {
		if (instance == null) {
			Log.d("DefaultRestTemplate", "creating instance");
			instance = new RestTemplate();
			instance.getMessageConverters().add(new StringHttpMessageConverter());
		}
		return instance;
	}
}
