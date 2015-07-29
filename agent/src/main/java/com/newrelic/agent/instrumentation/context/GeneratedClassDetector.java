package com.newrelic.agent.instrumentation.context;

import java.util.regex.Pattern;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;

import com.newrelic.agent.Agent;

public class GeneratedClassDetector implements ClassMatchVisitorFactory {
    static final boolean isGenerated(String className) {
        Pattern proxy = Pattern.compile("(.*)proxy(.*)", 2);
        Pattern cglib = Pattern.compile("(.*)cglib(.*)", 2);
        Pattern generated = Pattern.compile("(.*)generated(.*)", 2);

        if (className == null) {
            return false;
        }

        if (className.contains("$$")) {
            return true;
        }

        return (generated.matcher(className).find()) || (cglib.matcher(className).find()) || (proxy.matcher(className)
                                                                                                      .find());
    }

    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, final ClassReader reader,
                                             ClassVisitor cv, final InstrumentationContext context) {
        return new ClassVisitor(Agent.ASM_LEVEL, cv) {
            public void visitSource(String source, String debug) {
                super.visitSource(source, debug);
                context.setSourceAttribute(true);
                if ("<generated>".equals(source)) {
                    context.setGenerated(true);
                }
            }

            public void visitEnd() {
                super.visitEnd();
                if (!context.hasSourceAttribute()) {
                    String className = reader.getClassName();

                    if (GeneratedClassDetector.isGenerated(className)) {
                        context.setGenerated(true);
                    }
                }
            }
        };
    }
}