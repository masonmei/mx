//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.api;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;

public class ApiImplementationUpdate implements ContextClassTransformer {
    private final DefaultApiImplementations defaultImplementations = new DefaultApiImplementations();
    private final ClassMatchVisitorFactory matcher = new ClassMatchVisitorFactory() {
        public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, ClassReader reader,
                                                 ClassVisitor cv, final InstrumentationContext context) {
            String[] arr$ = reader.getInterfaces();
            int len$ = arr$.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String name = arr$[i$];
                Map unmodifiableMethods =
                        (Map) ApiImplementationUpdate.this.defaultImplementations.getApiClassNameToDefaultMethods()
                                      .get(name);
                if (unmodifiableMethods != null) {
                    final HashMap methods = Maps.newHashMap(unmodifiableMethods);
                    cv = new ClassVisitor(Agent.ASM_LEVEL, cv) {
                        public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                                         String[] exceptions) {
                            methods.remove(new Method(name, desc));
                            return super.visitMethod(access, name, desc, signature, exceptions);
                        }

                        public void visitEnd() {
                            if (!methods.isEmpty()) {
                                context.putMatch(ApiImplementationUpdate.this.matcher,
                                                        new Match(ImmutableMultimap.<ClassAndMethodMatcher, String>of(),
                                                                         methods.keySet(), null));
                            }

                            super.visitEnd();
                        }
                    };
                }
            }

            return cv;
        }
    };

    protected ApiImplementationUpdate() throws Exception {
    }

    public static void setup(InstrumentationContextManager manager) throws Exception {
        ApiImplementationUpdate transformer = new ApiImplementationUpdate();
        manager.addContextClassTransformer(transformer.matcher, transformer);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
                            Match match) throws IllegalClassFormatException {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(1);
        ClassVisitor cv = writer;
        String[] arr$ = reader.getInterfaces();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            String name = arr$[i$];
            Map methods = (Map) this.defaultImplementations.getApiClassNameToDefaultMethods().get(name);
            if (methods != null) {
                final HashMap methodsToAdd = Maps.newHashMap(methods);
                cv = new ClassVisitor(Agent.ASM_LEVEL, (ClassVisitor) cv) {
                    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                                     String[] exceptions) {
                        methodsToAdd.remove(new Method(name, desc));
                        return super.visitMethod(access, name, desc, signature, exceptions);
                    }

                    public void visitEnd() {
                        if (!methodsToAdd.isEmpty()) {
                            HashMap missingMethods = Maps.newHashMap(methodsToAdd);
                            Iterator i$ = missingMethods.entrySet().iterator();

                            while (i$.hasNext()) {
                                Entry entry = (Entry) i$.next();
                                ((MethodNode) entry.getValue()).accept(this);
                            }
                        }

                        super.visitEnd();
                    }
                };
            }
        }

        reader.accept((ClassVisitor) cv, 8);
        return writer.toByteArray();
    }

    protected ClassMatchVisitorFactory getMatcher() {
        return this.matcher;
    }
}
