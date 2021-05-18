package com.ktully.nativeimage.micronaut;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@Controller("/hello")
public class GreetingMicronautController {
	
	private static final Logger logger = LoggerFactory.getLogger(GreetingMicronautController.class);
	
	private final GreetingMicronautService greetingMicronautservice;
	
	// OTel
	OpenTelemetry openTelemetry = OtelTracerConfig.OpenTelemetryConfig();
	Tracer tracer = openTelemetry.getTracer("com.ktully.nativeimage.springboot.restservice");
	
    public GreetingMicronautController(GreetingMicronautService greetingMicronautservice) { 
        this.greetingMicronautservice = greetingMicronautservice;
    }
	
    @Get
    public String getFromService(HttpHeaders headers) {
    	
    	logger.debug("Logging incoming HTTP Header Name-Value Pairs.");
    	for(String headerName : headers.names()) {
    		logger.debug("Header Name  = " + headerName);
    		logger.debug("Header Value = " + headers.get(headerName));
    	}
    	
    	return greetingMicronautservice.greeting();
    }
	

}
