package com.ktully.nativeimage.quarkus.restservice;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

@ApplicationScoped
public class RestClient {
	
	private ExecutorService executorService = Executors.newCachedThreadPool();
	private Client client;
	private String baseUrl;// = "http://localhost:8080";

	@Inject
    public RestClient() {
        baseUrl = "http://httpbin.org";
        client = ClientBuilder.newBuilder()
                .executorService(executorService)
                .build();
    }

	CompletionStage<String> get() {
		return client.target(baseUrl + "/get").request().rx().get(String.class);
	}

}
