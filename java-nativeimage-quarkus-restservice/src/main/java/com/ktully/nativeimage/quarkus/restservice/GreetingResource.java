package com.ktully.nativeimage.quarkus.restservice;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.logging.Logger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

@ApplicationScoped
@Path("/hello")
public class GreetingResource {

	@Inject
	RestClient client;
	
	// Grab the Resource Attribute values from environment variables
	String otelServiceName = System.getenv("OTEL_SERVICE_NAME");
	String otelServiceNamespace = System.getenv("OTEL_SERVICE_NAMESPACE");
	
	// Logger
	private static final Logger logger = Logger.getLogger(GreetingResource.class);

	/*
	 * The below OpenTelemetry SDK is built with the {@link OtelTracerConfig#OpenTelemetryConfig() OtelTracerConfig} config.
	 * The Resource Attributes for service.name and service.namespace are being provided via environment variables
	 */
	OpenTelemetry openTelemetry = OtelTracerConfig.OpenTelemetryConfig(otelServiceName, otelServiceNamespace);
	
	// GlobalOpenTelemetry initialized in Main method, so grabbing it for use here
	// Disabling this approach for the reasons stated in the {@link Main.MyApp#run() Main run} method.
	//OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
	
	// Instantiate Tracer for this Controller
	Tracer tracer = openTelemetry.getTracer("com.ktully.nativeimage.quarkus.restservice");

	/*
	 * Configuration for Context Propagation to be done via @RequestHeader
	 * extraction
	 */

	TextMapGetter<MultivaluedMap<String, String>> getter = new TextMapGetter<MultivaluedMap<String, String>>() {
		@Override
		public String get(MultivaluedMap<String, String> carrier, String key) {
			logger.debug("** Key Found = " + key);			
			logger.debug("** Key Value = " + carrier.get(key));
			if (carrier.get(key) == null) {
				return "";
			} else {
				//logger.debug("** Returning the context: " + carrier.get(key).get(0));
				return carrier.get(key).get(0);
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
	
	 TextMapSetter<MultivaluedMap<String, String>> setter = new TextMapSetter<MultivaluedMap<String, String>>() {
	 
	 @Override 
	 public void set(MultivaluedMap<String, String> carrier, String key, String value) {
		 logger.debug("Adding Header with Key = " + key);
		 logger.debug("Adding Header with Value = " + value);
		 carrier.add(key, value);
		 } 
	 };

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String hello(@Context HttpHeaders headers) {
		// MultivaluedMap<String, String> requestHeaders = headers.getRequestHeaders();

		io.opentelemetry.context.Context extractedContext = null;
		try {
			logger.debug("Trying to extact Context Propagation Headers.");

			extractedContext = openTelemetry.getPropagators().getTextMapPropagator()
					.extract(io.opentelemetry.context.Context.current(), headers.getRequestHeaders(), getter);

			logger.debug("The extractedContext = " + extractedContext.toString());
			if (extractedContext.equals(null)) {
				logger.info("extractedContext is null");
			}
		} catch (Exception e) {
			logger.error("Exception caught while extracting Context Propagators", e);
		}

		Span serverSpan = tracer.spanBuilder("HTTP GET /hello").setParent(extractedContext).setSpanKind(SpanKind.SERVER)
				.startSpan();
		try (Scope scope = serverSpan.makeCurrent()) {
			logger.info("Building Server Span \"serverSpan\".");

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

			// Here is the downstream HTTP call stuff //
			Span httpClientSpan = tracer.spanBuilder("HTTP GET httpbin.org/get").setSpanKind(SpanKind.CLIENT)
					.startSpan();
			try (Scope outgoingScope = httpClientSpan.makeCurrent()) {

				logger.debug("Building HTTP Client Span \"httpClientSpan\".");
				// Add Log Event to Client Span
				httpClientSpan.addEvent("Calling httpbin.org/get via jax.ws.rs HTTP Client");
				// Add the attributes defined in the Semantic Conventions
				httpClientSpan.setAttribute("http.method", "GET");
				httpClientSpan.setAttribute("http.scheme", "http");
				httpClientSpan.setAttribute("http.host", "httpbin.org");
				httpClientSpan.setAttribute("http.target", "/get");

				// Inject W3C Context Propagation Headers
				logger.debug("Trying to inject Context Propagation Headers.");
				MultivaluedMap<String, String> outboundHeaders = new MultivaluedHashMap<>();
				openTelemetry.getPropagators().getTextMapPropagator().inject(io.opentelemetry.context.Context.current(), outboundHeaders, setter);
				
				logger.debug("Here are the headers on the HTTP Request:");
				for (String str : outboundHeaders.keySet()) {
					logger.debug("** Added Header Key = " + str);
					logger.debug("** Added Header Value = " + outboundHeaders.getFirst(str));
				}

				logger.debug("Sending downstream call.");
				String response = client.get().toCompletableFuture().join();
				logger.debug("HTTP Client Call Response = " + response);
			} catch (Exception e) {
				httpClientSpan.addEvent("error");
				httpClientSpan.addEvent(e.toString());
				httpClientSpan.setAttribute("error", true);
				logger.error("Error during OT section, here it is!", e);
				// return new Greeting(counter.incrementAndGet(), e.getMessage());
			} finally {
				httpClientSpan.end();
			}

			return "Hello RESTEasy";
		} catch (Exception e) {
			logger.info("Exception caught attempting to create Span", e);
			return e.getMessage();
		} finally {
			if (serverSpan != null) {
				serverSpan.end();
			}
		}
	}
}