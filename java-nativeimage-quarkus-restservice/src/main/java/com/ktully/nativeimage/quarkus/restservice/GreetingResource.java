package com.ktully.nativeimage.quarkus.restservice;

import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hello")
public class GreetingResource {
	
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(GreetingResource.class);

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@Context  HttpHeaders headers) {
    	MultivaluedMap<String, String> rh = headers.getRequestHeaders();
    	
        String str = rh.entrySet()
                .stream()
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n"));

    	logger.info(str);
        return "Hello RESTEasy";
        
        
    }
}