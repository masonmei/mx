package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.FieldNode;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.api.agent.weaver.MatchType;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract interface WeavedClassInfo
{
  public abstract String getSuperName();

  public abstract MatchType getMatchType();

  public abstract Set<Method> getWeavedMethods();

  public abstract Map<Method, TraceDetails> getTracedMethods();

  public abstract Collection<FieldNode> getReferencedFields();

  public abstract MethodVisitor getMethodVisitor(String paramString, MethodVisitor paramMethodVisitor, int paramInt,
                                                 Method paramMethod);

  public abstract MethodVisitor getConstructorMethodVisitor(MethodVisitor paramMethodVisitor, String paramString1,
                                                            int paramInt, String paramString2, String paramString3);

  public abstract Map<String, FieldNode> getNewFields();

  public abstract boolean isSkipIfPresent();
}