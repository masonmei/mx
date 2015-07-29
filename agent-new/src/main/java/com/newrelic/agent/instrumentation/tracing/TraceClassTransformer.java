package com.newrelic.agent.instrumentation.tracing;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

public class TraceClassTransformer implements ContextClassTransformer {
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
                            Match match) throws IllegalClassFormatException {
        try {
            return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINE, "Unable to transform class " + className, t);
        }
        return null;
    }

    private byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                               ProtectionDomain protectionDomain, byte[] classfileBuffer,
                               InstrumentationContext context) throws IllegalClassFormatException {
        if (!context.isTracerMatch()) {
            return null;
        }

        Agent.LOG.debug("Instrumenting class " + className);
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(1);
        ClassVisitor cv = writer;

        cv = new TraceClassVisitor(cv, className, context);
        reader.accept(cv, 8);

        return writer.toByteArray();
    }
}