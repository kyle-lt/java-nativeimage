package com.ktully.nativeimage.quarkus.restservice;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.jboss.logging.Logger;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

// OTLP Exporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

@ApplicationScoped
public class OtelTracerConfig {
	
	//@Produces
	//OpenTelemetrySdk openTelemetrySdk;
	
	// Logger
	private static final Logger logger = Logger.getLogger(OtelTracerConfig.class);

	//public Tracer OtelTracer() throws Exception {
	public static OpenTelemetry OpenTelemetryConfig() {
		
	    // ** Create OTLP gRPC Span Exporter & BatchSpanProcessor **
		OtlpGrpcSpanExporter spanExporter =
	            OtlpGrpcSpanExporter.builder()
	            	.setEndpoint("http://host.docker.internal:4317")
	            	.setTimeout(2, TimeUnit.SECONDS).build();
	        BatchSpanProcessor spanProcessor =
	            BatchSpanProcessor.builder(spanExporter)
	                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
	                .build();	   
	        
		// This was working using OTEL_RESOURCE_ATTRIBUTES env var, but apparently not anymore with 0.15.0+
	    
	    /*
	     * Attempting to try OTEL_RESOURCE_ATTRIBUTES again, let's see what happens (see below in SdkTracerProvider instantiation)
	     */
		AttributeKey<String> myServiceName = AttributeKey.stringKey("service.name");
		AttributeKey<String> myServiceNamespace = AttributeKey.stringKey("service.namespace");
	    Resource serviceNameResource =
	                Resource.create(Attributes.of(myServiceName, "java-nativeimage-quarkus-restservice",
	                		myServiceNamespace, "kjt-java-nativeimage"));
		
		// Let's log the OTEL_RESOURCE_ATTRIBUTES env var value, if found
	    logger.info("#### Logging Env Var OTEL_RESOURCE_ATTRIBUTES, if present ####");
	    System.getenv().forEach((k, v) -> {	    	
	    	if (k.toString().equalsIgnoreCase("OTEL_RESOURCE_ATTRIBUTES")) {
	    		logger.info(k + ":" + v);
	    	}
	    });
	    
	    // Let's log the Resource Attribute otel.resource.attributes
	    logger.info("#### Logging System Property otel.resource.attributes, if present ####");
	    logger.info("otel.resource.attributes:" + System.getProperty("otel.resource.attributes", "DOES_NOT_EXIST"));
	    
	    // ** Create OpenTelemetry SdkTracerProvider
		// Use OTLP & Logging Exporters
	    // Use Service Name Resource (and attributes) defined above
		// Use AlwaysOn TraceConfig
        SdkTracerProvider sdkTracerProvider =
                SdkTracerProvider.builder()
                    .addSpanProcessor(spanProcessor) // OTLP
                    .addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
                    // Attempting to try OTEL_RESOURCE_ATTRIBUTES again, let's see what happens (see above in Resource instantiation)
                    // export OTEL_RESOURCE_ATTRIBUTES=service.name=derpDoesThisWorkEnvVar
                    // I am also trying System Property -Dotel.resource.attributes=service.name=derpDoesThisWorkSysProp
                    // According to [here](https://github.com/open-telemetry/opentelemetry-java/blob/4cb2a5f0a3c96dc9af34c7a744f8ccd44da98344/CHANGELOG.md)
                    // If the opentelemetry-sdk-extension-autoconfigure package is used, then Resource.getDefault() 
                    // should use the OTEL_RESOURCE_ATTRIBUTES env var to populate attributes - but it's not working!!!!
                    //.setResource(Resource.getDefault().merge(serviceNameResource))
                    .setResource(Resource.getDefault())
                    .setSampler(Sampler.alwaysOn())
                    .build();
	        
	    // ** Create OpenTelemetry SDK **
		// Use W3C Trace Context Propagation
        // Use the SdkTracerProvider instantiated above
	    OpenTelemetrySdk openTelemetrySdk =
	            OpenTelemetrySdk.builder()
                	.setTracerProvider(sdkTracerProvider)
	            	.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
	                .build();
	    			//.buildAndRegisterGlobal();  // can/should I use this?  Maybe later...
	    
	    
	    //  ** Create Shutdown Hook **
	    Runtime.getRuntime().addShutdownHook(new Thread(sdkTracerProvider::shutdown));
	    
	    return openTelemetrySdk;

	}

}
