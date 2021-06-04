package com.ktully.nativeimage.micronaut;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.reactivex.Maybe;

@Introspected
@Controller("/hello")
public class GreetingMicronautController {
	
	private static final Logger logger = LoggerFactory.getLogger(GreetingMicronautController.class);
	
	private final GreetingMicronautService greetingMicronautservice;
	
	// Inject HTTP Client for downstream call
	@Client("http://httpbin.org") @Inject RxHttpClient httpClient;
	
	// OTel
	OpenTelemetry openTelemetry = OtelTracerConfig.OpenTelemetryConfig();
	Tracer tracer = openTelemetry.getTracer("com.ktully.nativeimage.micronaut.restservice");
	
	/*
	 * Configuration for Context Propagation to be done via @RequestHeader
	 * extraction
	 */
	private static final TextMapGetter<HttpHeaders> getter = new TextMapGetter<HttpHeaders>() {
		@Override
		public String get(HttpHeaders carrier, String key) {
			logger.debug("Key found = " + key);
			logger.debug("Value = " + carrier.get(key));
			if (carrier.get(key) == null) {
				return "";
			} else {
				logger.debug("** Returning the context: " + carrier.get(key));
				return carrier.get(key);
			}
		}
		// 0.10.0 - didn't need this implementation for 0.8.0
		@Override
		public Iterable<String> keys(HttpHeaders carrier) {
			return carrier.names();
		}
	};
	
	/*
	 * Configuration for Context Propagation to be done via HttpHeaders injection
	 */
	
	 TextMapSetter<MutableHttpHeaders> setter = new TextMapSetter<MutableHttpHeaders>() {
		 @Override 
		 public void set(MutableHttpHeaders carrier, String key, String value) {
			 logger.debug("Adding Header with Key = " + key);
			 logger.debug("Adding Header with Value = " + value);
			 carrier.add(key, value);
			 } 
		 };
	
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
    	
    	MutableHttpHeaders mutableHttpHeaders = (MutableHttpHeaders) headers;
    	
		Context extractedContext = null;
		try {
			logger.debug("Trying to extact Context Propagation Headers.");
			extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
					.extract(Context.current(), headers, getter);
			
			logger.debug(extractedContext.toString());
			if (extractedContext.equals(null)) {
				logger.info("extractedContext is null");
			}
		} catch (Exception e) {
			logger.error("Exception caught while extracting Context Propagators", e);
		}
    	
    	// ****** OpenTelemetry ******
    	//Span serverSpan = tracer.spanBuilder("HTTP GET /hello").setSpanKind(SpanKind.SERVER).startSpan();
    	Span serverSpan = tracer.spanBuilder("HTTP GET /hello").setParent(extractedContext).setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope scope = serverSpan.makeCurrent()) {
			logger.debug("Trying to build Span and then make RestTemplate call downstream");
			
			// Add some "Events" (AKA logs) to the span
			serverSpan.addEvent("This is an event with no Attributes");
			AttributeKey<String> attrKey = AttributeKey.stringKey("attrKey");
			Attributes spanEventAttr = Attributes.of(attrKey, "attrVal");
			serverSpan.addEvent("This is an event with an Attributes String Array", spanEventAttr);
			
			// Add the attributes defined in the Semantic Conventions
			serverSpan.setAttribute("http.method", "GET");
			serverSpan.setAttribute("http.scheme", "http");
			serverSpan.setAttribute("http.host", "java-nativeimage-springboot-restservice");
			serverSpan.setAttribute("http.target", "/hello");

			
			// Here is the downstream HTTP call stuff
			Span httpClientSpan = tracer.spanBuilder("HTTP GET httpbin.org/get").setSpanKind(SpanKind.CLIENT)
					.startSpan();
			try (Scope outgoingScope = httpClientSpan.makeCurrent()) {
				// Add some important info to our Span
				httpClientSpan.addEvent("Calling quarkus/hello via RestTemplate"); // This ends up in "logs"
																							// section in
				// Add the attributes defined in the Semantic Conventions
				httpClientSpan.setAttribute("http.method", "GET");
				httpClientSpan.setAttribute("http.scheme", "http");
				httpClientSpan.setAttribute("http.host", "quarkus");
				httpClientSpan.setAttribute("http.target", "/hello");

				// 0.14.1
				openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), mutableHttpHeaders, setter);
				
				Maybe<String> responseMaybe = httpClient.retrieve("/get").firstElement();
				
				// Change to unblocking laterz...
				String response = responseMaybe.blockingGet();
				
				//logger.debug("Injecting headers for call from java-chain to downstream API.");
				//logger.debug("**** Here are the headers: " + headers.toString());
				//HttpEntity<String> entity = new HttpEntity<String>("parameters", propagationHeaders);

				// Make outgoing call via RestTemplate
				//ResponseEntity<String> response = restTemplate.exchange("http://host.docker.internal:8080/hello",
				//		HttpMethod.GET, entity, String.class);

				//String responseString = response.getBody();
				//logger.debug("Response from downstream: ");
				//logger.debug(responseString);
			} catch (Exception e) {
				httpClientSpan.addEvent("error");
				httpClientSpan.addEvent(e.toString());
				httpClientSpan.setAttribute("error", true);
				logger.error("Error during OT section, here it is!", e);
				return greetingMicronautservice.greeting();
			} finally {
				httpClientSpan.end();
			}
			return greetingMicronautservice.greeting();
		} catch (Exception e) {
			logger.error("Exception caught attempting to create Span", e);
			return greetingMicronautservice.greeting();
		} finally {
			if (serverSpan != null) {
				serverSpan.end();
			}
		}
    	
    }
	

}
