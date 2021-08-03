package com.ktully.nativeimage.quarkus.restservice;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;

@QuarkusMain
public class Main {
    public static void main(String... args) {
        Quarkus.run(MyApp.class, args);
    }

    public static class MyApp implements QuarkusApplication {

        @Override
        public int run(String... args) throws Exception {
        	        
            /*
             * DEBUGGING - Sanity check that environment configs are present
             */
        	System.out.println("Checking for OTel Configs...");
            System.out.println("#### Logging Env Var OTEL_SERVICE_NAME, OTEL_RESOURCE_ATTRIBUTES, if present ####");
    	    System.getenv().forEach((k, v) -> {	    	
    	    	if (k.toString().equalsIgnoreCase("OTEL_SERVICE_NAME")) {
    	    		System.out.println(k + ":" + v);
    	    	}
    	    	else if (k.toString().equalsIgnoreCase("OTEL_RESOURCE_ATTRIBUTES")) {
    	    		System.out.println(k + ":" + v);
    	    	}
    	    });

    	    System.out.println("#### Logging System Property otel.service.name, if present ####");
    	    System.out.println("otel.service.name:" + System.getProperty("otel.service.name", "DOES_NOT_EXIST"));
    	    
    	    System.out.println("#### Logging System Property otel.resource.attributes, if present ####");
    	    System.out.println("otel.resource.attributes:" + System.getProperty("otel.resource.attributes", "DOES_NOT_EXIST"));    	
    	    
    	    // Initialize OTel via environment-based configs
    	    System.out.println("Initializing OpenTelemetry");
    	    OpenTelemetrySdkAutoConfiguration.initialize();
    	    
    	    // Now, run the app as normal
            Quarkus.waitForExit();
            return 0;
        }
    }
}