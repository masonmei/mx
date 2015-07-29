package com.newrelic.agent.instrumentation.context;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;

public interface ClassMatchVisitorFactory {
    ClassVisitor newClassMatchVisitor(ClassLoader paramClassLoader, Class<?> paramClass, ClassReader paramClassReader,
                                      ClassVisitor paramClassVisitor,
                                      InstrumentationContext paramInstrumentationContext);
}