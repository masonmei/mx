package com.newrelic.agent.instrumentation.weaver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({java.lang.annotation.ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WeaveInstrumentation {
    public abstract String title();

    public abstract String version();
}