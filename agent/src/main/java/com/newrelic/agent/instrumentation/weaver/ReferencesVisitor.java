//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.api.agent.weaver.MatchType;

class ReferencesVisitor extends ClassVisitor {
    private final Map<String, Set<MethodWithAccess>> referencedClassMethods;
    private final Map<String, Set<MethodWithAccess>> referencedInterfaceMethods;
    private final WeavedClassInfo weaveDetails;
    private final IAgentLogger logger;
    private String className;

    public ReferencesVisitor(IAgentLogger logger, WeavedClassInfo weaveDetails, ClassVisitor classVisitor,
                             Map<String, Set<MethodWithAccess>> referencedClasses,
                             Map<String, Set<MethodWithAccess>> referencedInterfaces) {
        super(Agent.ASM_LEVEL, classVisitor);
        this.weaveDetails = weaveDetails;
        this.referencedClassMethods = referencedClasses;
        this.referencedInterfaceMethods = referencedInterfaces;
        this.logger = logger;
    }

    private static void addReference(Type type, MethodWithAccess method,
                                     Map<String, Set<MethodWithAccess>> references) {
        if (type.getSort() == 10) {
            String internalName = type.getInternalName();
            if (internalName != null) {
                Set<MethodWithAccess> referencedMethods = references.get(internalName);
                if (referencedMethods == null) {
                    referencedMethods = Sets.newHashSet();
                    references.put(internalName, referencedMethods);
                }

                if (method != null) {
                    ((Set) referencedMethods).add(method);
                }
            }

        } else if (type.getSort() == 9) {
            addReference(type.getElementType(), method, references);
        }
    }

    public MatchType getMatchType() {
        return this.weaveDetails == null ? null : this.weaveDetails.getMatchType();
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.addClassReference(Type.getObjectType(name), (MethodWithAccess) null);
        if (null != superName) {
            this.addClassReference(Type.getObjectType(superName), (MethodWithAccess) null);
        }

        String[] arr$ = interfaces;
        int len$ = interfaces.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            String interfaceName = arr$[i$];
            this.addInterfaceReference(Type.getObjectType(interfaceName), (MethodWithAccess) null);
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        this.addClassReference(Type.getType(desc), (MethodWithAccess) null);
        return super.visitField(access, name, desc, signature, value);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        Method method = new Method(name, desc);
        MethodWithAccess methodWithAccess = new MethodWithAccess((access & 8) == 8, method);
        this.addClassReference(method.getReturnType(), (MethodWithAccess) null);
        Type[] isAbstract = method.getArgumentTypes();
        int synthetic = isAbstract.length;

        for (int i$ = 0; i$ < synthetic; ++i$) {
            Type argType = isAbstract[i$];
            this.addClassReference(argType, (MethodWithAccess) null);
        }

        boolean var13 = (1024 & access) != 0;
        if (this.weaveDetails != null && (this.weaveDetails.getWeavedMethods().contains(method) || var13)) {
            if (MatchType.Interface.equals(this.getMatchType())) {
                this.addInterfaceReference(Type.getObjectType(this.className), methodWithAccess);
            } else {
                this.addClassReference(Type.getObjectType(this.className), methodWithAccess);
            }
        }

        if (var13) {
            return mv;
        } else {
            final boolean var14 = (4096 & access) != 0;
            mv = new MethodVisitor(Agent.ASM_LEVEL, mv) {
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    if (var14 && null != ReferencesVisitor.this.weaveDetails) {
                        ReferencesVisitor.this.logger
                                .warning(ReferencesVisitor.this.className + " references a synthetic method to access "
                                                 + owner + "." + name + desc
                                                 + ".  This will not work correctly in instrumented classes.");
                    }

                    if (!ReferencesVisitor.this.className.equals(owner) && ((opcode & 183) == 0
                                                                                    || !name.equals("<init>")
                                                                                    || !desc.equals("()V"))) {
                        MethodWithAccess method = new MethodWithAccess(184 == opcode, new Method(name, desc));
                        if (185 == opcode) {
                            ReferencesVisitor.this.addInterfaceReference(Type.getObjectType(owner), method);
                        } else {
                            ReferencesVisitor.this.addClassReference(Type.getObjectType(owner), method);
                        }
                    }

                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }

                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
            };
            return mv;
        }
    }

    public void visitEnd() {
        super.visitEnd();
        Iterator i$ = this.referencedInterfaceMethods.entrySet().iterator();

        Entry entry;
        Set methods;
        do {
            if (!i$.hasNext()) {
                return;
            }

            entry = (Entry) i$.next();
            methods = (Set) this.referencedClassMethods.remove(entry.getKey());
        } while (methods == null || methods.isEmpty());

        throw new InvalidReferenceException((String) entry.getKey() + " is referenced as a class when invoking methods "
                                                    + methods + ", but as an interface when invoking methods "
                                                    + entry.getValue());
    }

    private void addClassReference(Type type, MethodWithAccess method) {
        addReference(type, method, this.referencedClassMethods);
    }

    private void addInterfaceReference(Type type, MethodWithAccess method) {
        addReference(type, method, this.referencedInterfaceMethods);
    }
}
