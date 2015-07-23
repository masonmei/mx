package com.newrelic.agent.instrumentation.webservices;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.context.TraceDetailsList;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Strings;

public class RestAnnotationVisitor {
    private static final String PATH_DESCRIPTOR = Type.getObjectType("javax/ws/rs/Path").getDescriptor();
    private static final Map<String, String> OPERATION_DESCRIPTORS = getHttpMethods();

    public static ClassVisitor createClassVisitor(final String internalClassName, ClassVisitor cv,
                                                  final TraceDetailsList context) {
        return new ClassVisitor(Agent.ASM_LEVEL, cv) {
            String rootPath;

            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (RestAnnotationVisitor.PATH_DESCRIPTOR.equals(desc)) {
                    return new AnnotationVisitor(Agent.ASM_LEVEL, super.visitAnnotation(desc, visible)) {
                        public void visit(String name, Object value) {
                            if ("value".equals(name)) {
                                rootPath = ((String) value);
                            }
                            super.visit(name, value);
                        }
                    };
                }

                return super.visitAnnotation(desc, visible);
            }

            public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc,
                                             String signature, String[] exceptions) {
                return new MethodVisitor(Agent.ASM_LEVEL, super.visitMethod(access, methodName, methodDesc, signature,
                                                                          exceptions)) {
                    String methodPath;
                    String httpMethod;

                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        String theHttpMethod = (String) RestAnnotationVisitor.OPERATION_DESCRIPTORS.get(desc);
                        if (theHttpMethod != null) {
                            httpMethod = theHttpMethod;
                        } else if (RestAnnotationVisitor.PATH_DESCRIPTOR.equals(desc)) {
                            return new AnnotationVisitor(Agent.ASM_LEVEL, super.visitAnnotation(desc, visible)) {
                                public void visit(String name, Object value) {
                                    if ("value".equals(name)) {
                                        methodPath = ((String) value);
                                    }
                                    super.visit(name, value);
                                }
                            };
                        }

                        return super.visitAnnotation(desc, visible);
                    }

                    public void visitEnd() {
                        if (httpMethod != null) {
                            TraceDetailsBuilder builder =
                                    TraceDetailsBuilder.newBuilder().setInstrumentationType(InstrumentationType.BuiltIn)
                                            .setInstrumentationSourceName(RestAnnotationVisitor.class.getName())
                                            .setDispatcher(true);

                            if ((rootPath == null) && (methodPath == null)) {
                                builder.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false,
                                                                  "RestWebService",
                                                                  Type.getObjectType(internalClassName).getClassName()
                                                                          + "/" + methodName);
                            } else {
                                String fullPath = RestAnnotationVisitor.getPath(rootPath, methodPath, httpMethod);
                                builder.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false,
                                                                  "RestWebService", fullPath);
                            }

                            context.addTrace(new Method(methodName, methodDesc), builder.build());
                        }
                        super.visitEnd();
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

    private static Map<String, String> getHttpMethods() {
        Map methods = Maps.newHashMap();
        for (String httpMethod : Arrays.asList(new String[] {"PUT", "POST", "GET", "DELETE", "HEAD", "OPTIONS"})) {
            methods.put(Type.getObjectType("javax/ws/rs/" + httpMethod).getDescriptor(), httpMethod);
        }
        return ImmutableMap.copyOf(methods);
    }

    public final ClassMatchVisitorFactory getClassMatchVisitorFactory() {
        return new ClassMatchVisitorFactory() {
            public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
                                                     ClassReader reader, ClassVisitor cv,
                                                     InstrumentationContext context) {
                return RestAnnotationVisitor.createClassVisitor(reader.getClassName(), cv, context);
            }
        };
    }

    public final ClassMatchVisitorFactory getInterfaceMatchVisitorFactory(final InstrumentationContextManager
                                                                                  instrumentationContextManager) {
        final Pattern classMatcherPattern = getClassMatcherPattern();
        Agent.LOG
                .log(Level.FINEST, "REST interface matcher pattern: {0}", new Object[] {classMatcherPattern.pattern()});

        return new ClassMatchVisitorFactory() {
            public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
                                                     final ClassReader reader, ClassVisitor cv,
                                                     InstrumentationContext context) {
                if (!classMatcherPattern.matcher(reader.getClassName()).matches()) {
                    return cv;
                }

                final TraceList list = new TraceList();
                return new ClassVisitor(Agent.ASM_LEVEL, RestAnnotationVisitor
                                                        .createClassVisitor(reader.getClassName(), cv, list)) {
                    public void visitEnd() {
                        super.visitEnd();

                        if (list.methodToDetails != null) {
                            List methodMatchers = Lists.newArrayList();
                            for (Method method : list.methodToDetails.keySet()) {
                                methodMatchers.add(new ExactMethodMatcher(method.getName(), method.getDescriptor()));
                            }
                            ClassAndMethodMatcher matcher =
                                    new DefaultClassAndMethodMatcher(new InterfaceMatcher(reader.getClassName()),
                                                                            OrMethodMatcher
                                                                                    .getMethodMatcher(methodMatchers));

                            ClassMatchVisitorFactory classMatcher = OptimizedClassMatcherBuilder.newBuilder()
                                                                            .addClassMethodMatcher(new ClassAndMethodMatcher[] {matcher})
                                                                            .build();

                            InterfaceImplementationMatcher interfaceMatcher =
                                    new InterfaceImplementationMatcher(reader.getClassName(), list.methodToDetails);

                            instrumentationContextManager.addContextClassTransformer(classMatcher, interfaceMatcher);

                            reloadMatchingClasses(classMatcher);
                        }
                    }

                    private void reloadMatchingClasses(ClassMatchVisitorFactory classMatcher) {
                        ServiceFactory.getClassTransformerService()
                                .retransformMatchingClasses(Arrays.asList(new ClassMatchVisitorFactory[]
                                                                                  {classMatcher}));
                    }
                };
            }
        };
    }

    private Pattern getClassMatcherPattern() {
        try {
            List matchers = (List) ServiceFactory.getConfigService().getDefaultAgentConfig()
                                           .getValue("instrumentation.rest_annotations.class_filter",
                                                            Arrays.asList(new String[] {"com.*"}));

            matchers = Lists.transform(matchers, new Function<String, String>() {
                public String apply(String input) {
                    return input.replace('.', '/');
                }
            });
            return Strings.getPatternFromGlobs(matchers);
        } catch (Exception ex) {
        }
        return Pattern.compile("^com/.*");
    }

    private static class TraceList implements TraceDetailsList {
        Map<Method, TraceDetails> methodToDetails;

        public void addTrace(Method method, TraceDetails traceDetails) {
            if (methodToDetails == null) {
                methodToDetails = Maps.newHashMap();
            }
            methodToDetails.put(method, traceDetails);
        }
    }

    private static final class InterfaceImplementationMatcher implements ContextClassTransformer {
        private final Map<Method, TraceDetails> methodToDetails;
        private final String interfaceName;

        public InterfaceImplementationMatcher(String className, Map<Method, TraceDetails> methodToDetails) {
            interfaceName = className;
            this.methodToDetails = methodToDetails;
        }

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer,
                                InstrumentationContext context, OptimizedClassMatcher.Match match)
                throws IllegalClassFormatException {
            Agent.LOG.log(Level.FINEST, "REST annotation match for interface {0}, class {1}",
                                 new Object[] {interfaceName, className});

            context.addTracedMethods(methodToDetails);
            return null;
        }
    }
}