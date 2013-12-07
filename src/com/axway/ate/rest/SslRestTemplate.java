package com.axway.ate.rest;

import java.io.File;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.axway.ate.ssl.TrustedHttpRequestFactory;

import android.content.Context;
import android.util.Log;

public class SslRestTemplate extends RestTemplate {

	private static RestTemplate instance;
	
	public static RestTemplate getInstance(Context ctx) {
		if (instance == null) {
			Log.d("SslRestTemplate", "creating instance");
			instance = new RestTemplate();
			instance.setRequestFactory(new TrustedHttpRequestFactory(ctx));
			instance.getMessageConverters().add(new StringHttpMessageConverter());
		}
		return instance;
	}
}
