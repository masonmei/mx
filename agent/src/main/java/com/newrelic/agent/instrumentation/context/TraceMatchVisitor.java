//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.context;

import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import com.newrelic.agent.instrumentation.tracing.Annotation;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;

class TraceMatchVisitor implements ClassMatchVisitorFactory {
    private final AnnotationMatcher traceAnnotationMatcher;
    private final AnnotationMatcher ignoreTransactionAnnotationMatcher;
    private final AnnotationMatcher ignoreApdexAnnotationMatcher;

    public TraceMatchVisitor() {
        ConfigService configService = ServiceFactory.getConfigService();
        ClassTransformerConfig classTransformerConfig =
                configService.getDefaultAgentConfig().getClassTransformerConfig();
        this.traceAnnotationMatcher = classTransformerConfig.getTraceAnnotationMatcher();
        this.ignoreTransactionAnnotationMatcher = classTransformerConfig.getIgnoreTransactionAnnotationMatcher();
        this.ignoreApdexAnnotationMatcher = classTransformerConfig.getIgnoreApdexAnnotationMatcher();
    }

    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
                                             final ClassVisitor cv, final InstrumentationContext context) {
        return new ClassVisitor(Agent.ASM_LEVEL, cv) {
            String source;

            public void visitSource(String source, String debug) {
                super.visitSource(source, debug);
                this.source = source;
            }

            public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Agent.ASM_LEVEL, super.visitMethod(access, methodName, methodDesc, signature,
                                                                          exceptions)) {
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (TraceMatchVisitor.this.traceAnnotationMatcher.matches(desc)) {
                            Annotation node = new Annotation(super.visitAnnotation(desc, visible),
                                                                    Type.getDescriptor(Trace.class),
                                                                    TraceDetailsBuilder.newBuilder()
                                                                            .setInstrumentationType
                                                                                     (InstrumentationType
                                                                                              .TraceAnnotation)
                                                                            .setInstrumentationSourceName(source)) {
                                public void visitEnd() {
                                    context.putTraceAnnotation(new Method(methodName, methodDesc),
                                                                      this.getTraceDetails(true));
                                    super.visitEnd();
                                }
                            };
                            return node;
                        } else {
                            if (TraceMatchVisitor.this.ignoreApdexAnnotationMatcher.matches(desc)) {
                                context.addIgnoreApdexMethod(methodName, methodDesc);
                            }

                            if (TraceMatchVisitor.this.ignoreTransactionAnnotationMatcher.matches(desc)) {
                                context.addIgnoreTransactionMethod(methodName, methodDesc);
                            }

                            return super.visitAnnotation(desc, visible);
                        }
                    }
                };
            }
        };
    }
}
