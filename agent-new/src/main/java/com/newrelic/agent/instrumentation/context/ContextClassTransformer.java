package com.newrelic.agent.instrumentation.context;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;

public interface ContextClassTransformer {
    byte[] transform(ClassLoader paramClassLoader, String paramString, Class<?> paramClass,
                     ProtectionDomain paramProtectionDomain, byte[] paramArrayOfByte,
                     InstrumentationContext paramInstrumentationContext, Match paramMatch)
            throws IllegalClassFormatException;
}