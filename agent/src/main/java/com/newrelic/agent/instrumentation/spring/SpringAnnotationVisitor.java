package com.newrelic.agent.instrumentation.spring;

import java.util.Set;
import java.util.logging.Level;

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
import com.newrelic.agent.instrumentation.context.TraceDetailsList;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;

public class SpringAnnotationVisitor {
    private static final Set<String> CONTROLLER_DESCRIPTORS = ImmutableSet
                                                                      .of(Type.getObjectType
                                                                                       ("org/springframework/stereotype/Controller")
                                                                                  .getDescriptor(),
                                                                                 Type.getObjectType
                                                                                              ("org/springframework/web/bind/annotation/RestController")
                                                                                         .getDescriptor());

    private static final String REQUEST_MAPPING_DESCRIPTOR =
            Type.getObjectType("org/springframework/web/bind/annotation/RequestMapping").getDescriptor();

    public static ClassVisitor createClassVisitor(final String internalClassName, ClassVisitor cv,
                                                  final TraceDetailsList context) {
        return new ClassVisitor(Agent.ASM_LEVEL, cv) {
            private boolean isController = false;
            private String rootPath;

            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (SpringAnnotationVisitor.CONTROLLER_DESCRIPTORS.contains(desc)) {
                    isController = true;
                }
                if (SpringAnnotationVisitor.REQUEST_MAPPING_DESCRIPTOR.equals(desc)) {
                    av = new AnnotationVisitor(Agent.ASM_LEVEL, av) {
                        public AnnotationVisitor visitArray(String name) {
                            AnnotationVisitor av = super.visitArray(name);
                            if ("value".equals(name)) {
                                return new AnnotationVisitor(Agent.ASM_LEVEL, av) {
                                    public void visit(String name, Object value) {
                                        super.visit(name, value);
                                        rootPath = value.toString();
                                    }
                                };
                            }

                            return av;
                        }

                    };
                }

                return av;
            }

            public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc,
                                             String signature, String[] exceptions) {
                if (!isController) {
                    return super.visitMethod(access, methodName, methodDesc, signature, exceptions);
                }
                return new MethodVisitor(Agent.ASM_LEVEL, super.visitMethod(access, methodName, methodDesc, signature,
                                                                          exceptions)) {
                    String path;
                    String httpMethod = "GET";

                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (SpringAnnotationVisitor.REQUEST_MAPPING_DESCRIPTOR.equals(desc)) {
                            return new AnnotationVisitor(Agent.ASM_LEVEL, super.visitAnnotation(desc, visible)) {
                                public AnnotationVisitor visitArray(String name) {
                                    AnnotationVisitor av = super.visitArray(name);
                                    if ("value".equals(name)) {
                                        return new AnnotationVisitor(Agent.ASM_LEVEL, av) {
                                            public void visit(String name, Object value) {
                                                super.visit(name, value);
                                                path = value.toString();
                                            }
                                        };
                                    }
                                    if ("method".equals(name)) {
                                        return new AnnotationVisitor(Agent.ASM_LEVEL, av) {
                                            public void visitEnum(String name, String desc, String value) {
                                                super.visitEnum(name, desc, value);
                                                httpMethod = value;
                                            }
                                        };
                                    }

                                    return av;
                                }

                                public void visitEnd() {
                                    super.visitEnd();

                                    if ((path == null) && (rootPath == null)) {
                                        Agent.LOG.log(Level.FINE, "No path was specified for SpringController {0}",
                                                             new Object[] {internalClassName});
                                    } else {
                                        TraceDetailsBuilder builder = TraceDetailsBuilder.newBuilder()
                                                                              .setInstrumentationType
                                                                                       (InstrumentationType.BuiltIn)
                                                                              .setInstrumentationSourceName
                                                                                       (SpringAnnotationVisitor.class
                                                                                                                    .getName())
                                                                              .setDispatcher(true);

                                        String fullPath = SpringAnnotationVisitor.getPath(rootPath, path, httpMethod);
                                        builder.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, true,
                                                                          "SpringController", fullPath);

                                        context.addTrace(new Method(methodName, methodDesc), builder.build());
                                    }
                                }
                            };
                        }

                        return super.visitAnnotation(desc, visible);
                    }
                };
            }
        };
    }

    static String getPath(String rootPath, String methodPath, String httpMethod) {
        StringBuilder fullPath = new StringBuilder();
        if (rootPath != null) {
            if (rootPath.endsWith("/")) {
                fullPath.append(rootPath.substring(0, rootPath.length() - 1));
            } else {
                fullPath.append(rootPath);
            }
        }
        if (methodPath != null) {
            if (!methodPath.startsWith("/")) {
                fullPath.append('/');
            }
            if (methodPath.endsWith("/")) {
                fullPath.append(methodPath.substring(0, methodPath.length() - 1));
            } else {
                fullPath.append(methodPath);
            }
        }

        fullPath.append(" (").append(httpMethod).append(')');
        return fullPath.toString();
    }

    public final ClassMatchVisitorFactory getClassMatchVisitorFactory() {
        return new ClassMatchVisitorFactory() {
            public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
                                                     ClassReader reader, ClassVisitor cv,
                                                     InstrumentationContext context) {
                return SpringAnnotationVisitor.createClassVisitor(reader.getClassName(), cv, context);
            }
        };
    }
}