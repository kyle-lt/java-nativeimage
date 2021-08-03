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
	private String baseUrl;

	@Inject
    public RestClient() {
        //baseUrl = "http://host.docker.internal:8082";
		// Changing to httpbin.org to remove dependency of micronaut service
		baseUrl = "http://httpbin.org";
        client = ClientBuilder.newBuilder()
                .executorService(executorService)
                .build();
    }

	CompletionStage<String> get() {		
		//return client.target(baseUrl + "/hello").request().rx().get(String.class);
		// Changing to httpbin/get to remove dependency of micronaut service
		return client.target(baseUrl + "/get").request().rx().get(String.class);
	}

}
