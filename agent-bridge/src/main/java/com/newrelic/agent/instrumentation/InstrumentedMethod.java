package com.newrelic.agent.instrumentation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InstrumentedMethod {
    boolean dispatcher() default false;

    InstrumentationType[] instrumentationTypes();

    String[] instrumentationNames();
}