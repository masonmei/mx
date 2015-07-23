package com.newrelic.agent.servlet;

import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;

public class ServletAnnotationVisitor implements ClassMatchVisitorFactory {
    private static final String WEB_SERVLET_DESCRIPTOR =
            Type.getObjectType("javax/servlet/annotation/WebServlet").getDescriptor();

    private static final Set<String> SERVLET_METHODS =
            ImmutableSet.of("service", "doGet", "doPost", "doHead", "doPut", "doOptions", new String[] {"doTrace"});

    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
                                             ClassVisitor cv, final InstrumentationContext context) {
        cv = new ClassVisitor(Agent.ASM_LEVEL, cv) {
            private TraceDetails traceDetails;

            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                if ((traceDetails != null) && (ServletAnnotationVisitor.SERVLET_METHODS.contains(name))) {
                    context.addTrace(new Method(name, desc), traceDetails);
                }

                return mv;
            }

            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (ServletAnnotationVisitor.WEB_SERVLET_DESCRIPTOR.equals(desc)) {
                    return new AnnotationVisitor(Agent.ASM_LEVEL, super.visitAnnotation(desc, visible)) {
                        String[] urlPatterns;

                        public AnnotationVisitor visitArray(String name) {
                            AnnotationVisitor av = super.visitArray(name);
                            if (("value".equals(name)) || ("urlPatterns".equals(name))) {
                                av = new AnnotationVisitor(Agent.ASM_LEVEL, av) {
                                    public void visit(String name, Object value) {
                                        super.visit(name, value);
                                        if (urlPatterns == null) {
                                            urlPatterns = new String[] {value.toString()};
                                        }
                                    }
                                };
                            }

                            return av;
                        }

                        public void visitEnd() {
                            super.visitEnd();

                            if (urlPatterns != null) {
                                traceDetails = TraceDetailsBuilder.newBuilder()
                                                       .setInstrumentationType(InstrumentationType.BuiltIn)
                                                       .setInstrumentationSourceName(ServletAnnotationVisitor.class
                                                                                             .getName())
                                                       .setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false,
                                                                                  "WebServletPath", urlPatterns[0])
                                                       .build();
                            }

                        }

                    };
                }

                return super.visitAnnotation(desc, visible);
            }
        };
        return cv;
    }
}