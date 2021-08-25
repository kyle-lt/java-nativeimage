package com.ktully.nativeimage.quarkus.restservice;

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

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

import org.eclipse.microprofile.config.inject.ConfigProperty;

// OTLP Exporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

@ApplicationScoped
public class OtelTracerConfig {

	// @Produces
	// OpenTelemetrySdk openTelemetrySdk;

	// Logger
	private static final Logger logger = Logger.getLogger(OtelTracerConfig.class);

	// Using service resource attributes provided by environment variables
	@ConfigProperty(name = "otel.service.name")
	private static String otelServiceName;
	@ConfigProperty(name = "otel.service.namespace")
	private static String otelServiceNamespace;

	// public Tracer OtelTracer() throws Exception {
	public static OpenTelemetry OpenTelemetryConfig() {

		// ** Create OTLP gRPC Span Exporter & BatchSpanProcessor **
		OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
				.setEndpoint("http://host.docker.internal:4317").setTimeout(2, TimeUnit.SECONDS).build();
		BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter)
				.setScheduleDelay(100, TimeUnit.MILLISECONDS).build();

		/*
		 * Attempting to try OTEL_RESOURCE_ATTRIBUTES again, let's see what happens (see
		 * below in SdkTracerProvider instantiation)
		 */
		AttributeKey<String> myServiceName = AttributeKey.stringKey("service.name");
		AttributeKey<String> myServiceNamespace = AttributeKey.stringKey("service.namespace");
		Resource serviceNameResource = Resource
				.create(Attributes.of(myServiceName, otelServiceName, myServiceNamespace, otelServiceNamespace));

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
		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder().addSpanProcessor(spanProcessor) // OTLP
				.addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()))
				.setResource(Resource.getDefault().merge(serviceNameResource))
				// .setResource(Resource.getDefault())
				.setSampler(Sampler.alwaysOn()).build();

		// ** Create OpenTelemetry SDK **
		// Use W3C Trace Context Propagation
		// Use the SdkTracerProvider instantiated above
		OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider)
				.setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance())).build();

		// ** Create Shutdown Hook **
		Runtime.getRuntime().addShutdownHook(new Thread(sdkTracerProvider::shutdown));

		return openTelemetrySdk;

	}

}
