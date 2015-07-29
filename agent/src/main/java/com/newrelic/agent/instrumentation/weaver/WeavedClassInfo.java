package com.newrelic.agent.instrumentation.weaver;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.FieldNode;

public interface WeavedClassInfo {
    String getSuperName();

    MatchType getMatchType();

    Set<Method> getWeavedMethods();

    Map<Method, TraceDetails> getTracedMethods();

    Collection<FieldNode> getReferencedFields();

    MethodVisitor getMethodVisitor(String paramString, MethodVisitor paramMethodVisitor, int paramInt,
                                   Method paramMethod);

    MethodVisitor getConstructorMethodVisitor(MethodVisitor paramMethodVisitor, String paramString1, int paramInt,
                                              String paramString2, String paramString3);

    Map<String, FieldNode> getNewFields();

    boolean isSkipIfPresent();
}