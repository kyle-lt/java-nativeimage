package com.ktully.nativeimage.quarkus.restservice;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
//import io.opentelemetry.context.propagation.TextMapSetter;

//@ApplicationScoped
@Path("/hello")
public class GreetingResource {
	
	// Logger
	private static final Logger logger = LoggerFactory.getLogger(GreetingResource.class);
	
	//@Inject
	// OTel
	//private static final OpenTelemetry openTelemetry = OtelTracerConfig.OpenTelemetryConfig();
	OpenTelemetry openTelemetry = OtelTracerConfig.OpenTelemetryConfig();
	//private static final Tracer tracer =
	//	      openTelemetry.getTracer("com.ktully.nativeimage.springboot.restservice");
	Tracer tracer =
		      openTelemetry.getTracer("com.ktully.nativeimage.springboot.restservice");
	
	/*
	 * Configuration for Context Propagation to be done via @RequestHeader
	 * extraction
	 */
	
	private static final TextMapGetter<MultivaluedMap<String, String>> getter = new TextMapGetter<MultivaluedMap<String, String>>() {
		@Override
		public String get(MultivaluedMap<String, String> carrier, String key) {
			logger.info("Key = " + key);
			logger.info("Key found! " + key);
			logger.info("Value = " + carrier.get(key));
			if (carrier.get(key) == null) {
				return "";
			}
			else {
				return carrier.get(key).toString();
			}
		}
		// 0.10.0 - didn't need this implementation for 0.8.0
		@Override
		public Iterable<String> keys(MultivaluedMap<String, String> carrier) {
			return carrier.keySet();
		}
	};
	

	/*
	 * Configuration for Context Propagation to be done via HttpHeaders injection
	 */
	/*
	private static final TextMapSetter<HttpHeaders> httpHeadersSetter = new TextMapSetter<HttpHeaders>() {
		@Override
		public void set(HttpHeaders carrier, String key, String value) {
			logger.debug("RestTemplate - Adding Header with Key = " + key);
			logger.debug("RestTemplate - Adding Header with Value = " + value);
			carrier.set(key, value);
		}
	};
	*/

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@Context  HttpHeaders headers) {
    	MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();
        /*
    	String str = requestHeaders.entrySet()
                .stream()
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n"));
    	logger.info(str);
    	*/
    	
    	io.opentelemetry.context.Context extractedContext = null;
		try {
			logger.info("Trying to extact Context Propagation Headers.");
			extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
					.extract(io.opentelemetry.context.Context.current(), requestHeaders, getter);
			
			logger.info(extractedContext.toString());
		} catch (Exception e) {
			logger.error("Exception caught while extracting Context Propagators", e);
		}
		
		Span serverSpan = tracer.spanBuilder("HTTP GET /hello").setParent(extractedContext).setSpanKind(SpanKind.SERVER).startSpan();
		try (Scope scope = serverSpan.makeCurrent()) {
			logger.info("Trying to build Server Span.");
			
			// Add some "Events" (AKA logs) to the span
			serverSpan.addEvent("This is an event with no Attributes");
			AttributeKey<String> attrKey = AttributeKey.stringKey("attrKey");
			Attributes spanEventAttr = Attributes.of(attrKey, "attrVal");
			serverSpan.addEvent("This is an event with an Attributes String Array", spanEventAttr);
			
			// Add the attributes defined in the Semantic Conventions
			serverSpan.setAttribute("http.method", "GET");
			serverSpan.setAttribute("http.scheme", "http");
			serverSpan.setAttribute("http.host", "java-nativeimage-quarkus-restservice");
			serverSpan.setAttribute("http.target", "/hello");

			//RestTemplate restTemplate = new RestTemplate();
			//HttpHeaders propagationHeaders = new HttpHeaders();

			/*
			Span restTemplateSpan = tracer.spanBuilder("HTTP GET httpbin.org/get").setSpanKind(SpanKind.CLIENT).startSpan();
			try (Scope outgoingScope = restTemplateSpan.makeCurrent()) {
				// Add some important info to our Span
				restTemplateSpan.addEvent("Calling httpbin.org/get via RestTemplate"); // This ends up in "logs"
																							// section in
				// Add the attributes defined in the Semantic Conventions
				restTemplateSpan.setAttribute("http.method", "GET");
				restTemplateSpan.setAttribute("http.scheme", "http");
				restTemplateSpan.setAttribute("http.host", "httpbin.org");
				restTemplateSpan.setAttribute("http.target", "/get");

				// 0.14.1
				openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), propagationHeaders, httpHeadersSetter);
				
				logger.debug("Injecting headers for call from java-chain to downstream API.");
				logger.debug("**** Here are the headers: " + headers.toString());
				HttpEntity<String> entity = new HttpEntity<String>("parameters", propagationHeaders);

				// Make outgoing call via RestTemplate
				ResponseEntity<String> response = restTemplate.exchange("http://httpbin.org/get",
						HttpMethod.GET, entity, String.class);

				String responseString = response.getBody();
				logger.debug("Response from downstream: ");
				logger.debug(responseString);
			} catch (Exception e) {
				restTemplateSpan.addEvent("error");
				restTemplateSpan.addEvent(e.toString());
				restTemplateSpan.setAttribute("error", true);
				logger.error("Error during OT section, here it is!", e);
				return new Greeting(counter.incrementAndGet(), e.getMessage());
			} finally {
				restTemplateSpan.end();
			}
			*/
		
			return "Hello RESTEasy";
		} catch (Exception e) {
			logger.error("Exception caught attempting to create Span", e);
			return e.getMessage();
		} finally {
			if (serverSpan != null) {
				serverSpan.end();
			}
		}
    }
}