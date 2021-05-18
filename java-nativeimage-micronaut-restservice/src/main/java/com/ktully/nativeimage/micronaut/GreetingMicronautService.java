package com.ktully.nativeimage.micronaut;

import javax.inject.Singleton;

@Singleton 
public class GreetingMicronautService {

    public String greeting() { 
        return "Hello from Micronaut!";
    }
}
