package com.newrelic.agent.config;

import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import java.util.Collection;
import java.util.Set;

public abstract interface ClassTransformerConfig extends Config
{
  public abstract boolean isCustomTracingEnabled();

  public abstract Set<String> getExcludes();

  public abstract Set<String> getIncludes();

  public abstract boolean computeFrames();

  public abstract long getShutdownDelayInNanos();

  public abstract boolean isEnabled();

  public abstract Collection<String> getJdbcStatements();

  public abstract AnnotationMatcher getIgnoreTransactionAnnotationMatcher();

  public abstract AnnotationMatcher getIgnoreApdexAnnotationMatcher();

  public abstract AnnotationMatcher getTraceAnnotationMatcher();

  public abstract boolean isGrantPackageAccess();

  public abstract Config getInstrumentationConfig(String paramString);
}