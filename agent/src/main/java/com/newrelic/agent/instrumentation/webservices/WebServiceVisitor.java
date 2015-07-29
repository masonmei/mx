//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.webservices;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import javax.jws.WebMethod;
import javax.jws.WebService;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.AnnotationDetails;
import com.newrelic.agent.util.asm.ClassStructure;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class WebServiceVisitor implements ClassMatchVisitorFactory {
    private static final String WEB_SERVICE_ANNOTATION_DESCRIPTOR = Type.getDescriptor(WebService.class);

    public WebServiceVisitor() {
    }

    public ClassVisitor newClassMatchVisitor(final ClassLoader loader, Class<?> classBeingRedefined,
                                             final ClassReader reader, final ClassVisitor cv,
                                             final InstrumentationContext context) {
        return reader.getInterfaces().length == 0 ? null : new ClassVisitor(Agent.ASM_LEVEL, cv) {
            Map<Method, AnnotationDetails> methodsToInstrument;
            Map<String, String> classWebServiceAnnotationDetails;
            String webServiceAnnotationNameValue;

            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (!WebServiceVisitor.WEB_SERVICE_ANNOTATION_DESCRIPTOR.equals(desc)) {
                    return super.visitAnnotation(desc, visible);
                } else {
                    this.methodsToInstrument = Maps.newHashMap();
                    this.classWebServiceAnnotationDetails = Maps.newHashMap();
                    String[] arr$ = reader.getInterfaces();
                    int len$ = arr$.length;

                    for (int i$ = 0; i$ < len$; ++i$) {
                        String interfaceName = arr$[i$];

                        try {
                            ClassStructure e =
                                    ClassStructure.getClassStructure(Utils.getClassResource(loader, interfaceName), 15);
                            AnnotationDetails webServiceDetails = (AnnotationDetails) e.getClassAnnotations()
                                                                                              .get(WebServiceVisitor
                                                                                                           .WEB_SERVICE_ANNOTATION_DESCRIPTOR);
                            if (webServiceDetails != null) {
                                this.webServiceAnnotationNameValue = (String) webServiceDetails.getValue("name");
                                Iterator i$1 = e.getMethods().iterator();

                                while (i$1.hasNext()) {
                                    Method m = (Method) i$1.next();
                                    Map methodAnnotations = e.getMethodAnnotations(m);
                                    AnnotationDetails webMethodDetails = (AnnotationDetails) methodAnnotations
                                                                                                     .get(Type.getDescriptor(WebMethod.class));
                                    this.methodsToInstrument.put(m, webMethodDetails);
                                }
                            }
                        } catch (Exception var13) {
                            Agent.LOG.log(Level.FINEST, var13.toString(), var13);
                        }
                    }

                    return new WebServiceAnnotationVisitor(super.visitAnnotation(desc, visible));
                }
            }

            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                if (this.methodsToInstrument != null) {
                    Method method = new Method(name, desc);
                    if (this.methodsToInstrument.containsKey(method)) {
                        AnnotationDetails webMethod = (AnnotationDetails) this.methodsToInstrument.get(method);
                        String className = (String) this.classWebServiceAnnotationDetails.get("endpointInterface");
                        if (className == null) {
                            className = Type.getObjectType(reader.getClassName()).getClassName();
                        }

                        String operationName = webMethod == null ? name : (String) webMethod.getValue("operationName");
                        if (operationName == null) {
                            operationName = name;
                        }

                        String txName = Strings.join('/', new String[] {className, operationName});
                        if (Agent.LOG.isFinerEnabled()) {
                            Agent.LOG.finer("Creating a web service tracer for " + reader.getClassName() + '.' + method
                                                    + " using transaction name " + txName);
                        }

                        context.addTrace(method, TraceDetailsBuilder.newBuilder()
                                                         .setInstrumentationType(InstrumentationType.BuiltIn)
                                                         .setInstrumentationSourceName(WebServiceVisitor.class
                                                                                               .getName())
                                                         .setDispatcher(true).setWebTransaction(true)
                                                         .setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH,
                                                                                    false, "WebService", txName)
                                                         .build());
                    }
                }

                return super.visitMethod(access, name, desc, signature, exceptions);
            }

            class WebServiceAnnotationVisitor extends AnnotationVisitor {
                public WebServiceAnnotationVisitor(AnnotationVisitor av) {
                    super(Agent.ASM_LEVEL, av);
                }

                public void visit(String name, Object value) {
                    if (value instanceof String) {
                        classWebServiceAnnotationDetails.put(name, (String) value);
                    }

                    super.visit(name, value);
                }
            }
        };
    }
}
