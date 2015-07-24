package com.newrelic.agent.instrumentation.context;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public interface ClassMatchVisitorFactory {
    ClassVisitor newClassMatchVisitor(ClassLoader paramClassLoader, Class<?> paramClass, ClassReader paramClassReader,
                                      ClassVisitor paramClassVisitor,
                                      InstrumentationContext paramInstrumentationContext);
}