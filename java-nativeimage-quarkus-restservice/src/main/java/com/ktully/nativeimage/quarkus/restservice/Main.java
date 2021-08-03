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
            System.out.println("Initializing OpenTelemetry");
        	OpenTelemetrySdkAutoConfiguration.initialize();
            Quarkus.waitForExit();
            return 0;
        }
    }
}