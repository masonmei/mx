package com.newrelic.agent.instrumentation.ejb3;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.deps.com.google.common.collect.ImmutableSet;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class EJBAnnotationVisitor implements ClassMatchVisitorFactory {
    private static final Set<String> EJB_DESCRIPTORS = ImmutableSet.of(Type.getObjectType("javax/ejb/Stateless")
                                                                               .getDescriptor(),
                                                                              Type.getObjectType("javax/ejb/Stateful")
                                                                                      .getDescriptor());

    private static final String EJB_REMOTE_INTERFCE_DESCRIPTOR = Type.getObjectType("javax/ejb/Remote").getDescriptor();
    private static final String EJB_LOCAL_INTERFCE_DESCRIPTOR = Type.getObjectType("javax/ejb/Local").getDescriptor();

    private static final Object EJB_INTERFACE = Type.getObjectType("javax/ejb/SessionBean");

    public ClassVisitor newClassMatchVisitor(final ClassLoader loader, Class<?> classBeingRedefined,
                                             final ClassReader reader, ClassVisitor cv,
                                             final InstrumentationContext context) {
        return new ClassVisitor(327680, cv) {
            Set<Method> methodsToInstrument = new HashSet();

            public void visit(int version, int access, String name, String signature, String superName,
                              String[] interfaces) {
                for (String interfaceName : interfaces) {
                    if (interfaceName.equals(EJBAnnotationVisitor.EJB_INTERFACE)) {
                        try {
                            ClassStructure classStructure =
                                    ClassStructure.getClassStructure(Utils.getClassResource(loader, interfaceName), 15);

                            collectMethodsToInstrument(classStructure);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (EJBAnnotationVisitor.EJB_DESCRIPTORS.contains(desc)) {
                    for (String interfaceName : reader.getInterfaces()) {
                        try {
                            ClassStructure classStructure =
                                    ClassStructure.getClassStructure(Utils.getClassResource(loader, interfaceName), 15);

                            Map annotations = classStructure.getClassAnnotations();
                            if ((annotations.containsKey(EJBAnnotationVisitor.EJB_REMOTE_INTERFCE_DESCRIPTOR))
                                        || (annotations
                                                    .containsKey(EJBAnnotationVisitor.EJB_LOCAL_INTERFCE_DESCRIPTOR))) {
                                collectMethodsToInstrument(classStructure);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return super.visitAnnotation(desc, visible);
            }

            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                Method method = new Method(name, desc);
                if (this.methodsToInstrument.contains(method)) {
                    if (Agent.LOG.isFinerEnabled()) {
                        Agent.LOG.finer("Creating a tracer for " + reader.getClassName() + '.' + method);
                    }
                    context.addTrace(method, TraceDetailsBuilder.newBuilder()
                                                     .setInstrumentationType(InstrumentationType.BuiltIn)
                                                     .setInstrumentationSourceName(EJBAnnotationVisitor.class.getName())
                                                     .build());
                }

                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            private void collectMethodsToInstrument(ClassStructure classStructure) {
                for (Method m : classStructure.getMethods()) {
                    this.methodsToInstrument.add(m);
                }
            }
        };
    }
}