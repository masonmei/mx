package com.newrelic.agent.instrumentation.pointcuts;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** @deprecated */
@LoadOnBootstrap
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceMixin
{
  public abstract String[] originalClassName();
}