package com.newrelic.agent.instrumentation.weaver;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.newrelic.deps.org.objectweb.asm.tree.FrameNode;
import com.newrelic.deps.org.objectweb.asm.tree.InnerClassNode;
import com.newrelic.deps.org.objectweb.asm.tree.InsnNode;
import com.newrelic.deps.org.objectweb.asm.tree.LabelNode;
import com.newrelic.deps.org.objectweb.asm.tree.LineNumberNode;
import com.newrelic.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.newrelic.deps.org.objectweb.asm.tree.MethodNode;
import com.newrelic.deps.org.objectweb.asm.util.TraceClassVisitor;

class MixinClassVisitor extends ClassVisitor {
    private final Map<Method, MergeMethodVisitor> methods = Maps.newHashMap();
    private final byte[] bytes;
    private final List<InnerClassNode> innerClasses = Lists.newArrayList();
    private final Map<Method, MethodNode> methodsToInline = Maps.newHashMap();
    private final InstrumentationPackage instrumentationPackage;
    private final WeavedClassInfo weavedClassInfo;
    String className;
    String[] interfaces;

    public MixinClassVisitor(byte[] bytes, InstrumentationPackage instrumentationPackage,
                             WeavedClassInfo weavedClassInfo) {
        super(327680);
        this.bytes = bytes;
        this.instrumentationPackage = instrumentationPackage;
        this.weavedClassInfo = weavedClassInfo;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.interfaces = interfaces;
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        this.innerClasses.add(new InnerClassNode(name, outerName, innerName, access));
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        Method method = new Method(name, desc);
        if ((0x440 & access) != 0) {
            return null;
        }

        if ("<clinit>".equals(name)) {
            this.instrumentationPackage.getLogger()
                    .warning(this.className + " in " + this.instrumentationPackage.implementationTitle
                                     + " contains a class constructor (static initializer).  This code will be "
                                     + "discarded.");

            return null;
        }

        MergeMethodVisitor node =
                new MergeMethodVisitor(this.instrumentationPackage, this.className, 327680, access, name, desc,
                                              signature, exceptions);

        this.methods.put(method, node);

        return node;
    }

    public void visitEnd() {
        super.visitEnd();

        removeInitSuperCalls();

        List emptyMethods = Lists.newArrayList();
        for (Entry<Method, MergeMethodVisitor> entry : methods.entrySet()) {
            if ((entry.getValue()).instructions.size() == 0) {
                emptyMethods.add(entry.getKey());
            } else if (entry.getValue().isNewMethod()) {
                this.methodsToInline.put(entry.getKey(), entry.getValue());
            }

        }

        this.methods.keySet().removeAll(emptyMethods);
    }

    private void removeInitSuperCalls() {
        for (MergeMethodVisitor methodNode : this.methods.values()) {
            if (MergeMethodVisitor.isInitMethod(methodNode.name)) {
                removeInitSuperCalls(methodNode);
                removeEmptyInitMethod(methodNode);
            }
        }
    }

    private void removeEmptyInitMethod(MergeMethodVisitor methodNode) {
        AbstractInsnNode[] insnNodes = methodNode.instructions.toArray();
        for (AbstractInsnNode node : insnNodes) {
            if ((!(node instanceof FrameNode)) && (!(node instanceof LabelNode))
                        && (!(node instanceof LineNumberNode))) {
                if ((!(node instanceof InsnNode)) || (node.getOpcode() != 177)) {
                    this.instrumentationPackage.getLogger()
                            .finest("Keeping <init> method after encountering opcode " + node.getOpcode());

                    return;
                }
            }
        }
        this.instrumentationPackage.getLogger().finest("Discarding <init> method on weaved class " + this.className);
        methodNode.instructions.clear();
    }

    private void removeInitSuperCalls(MergeMethodVisitor methodNode) {
        AbstractInsnNode[] insnNodes = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : insnNodes) {
            methodNode.instructions.remove(insn);

            if (insn.getOpcode() == 183) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
                if (methodInsnNode.owner.equals(this.weavedClassInfo.getSuperName())) {
                    return;
                }
            }
        }

        throw new IllegalInstructionException("Error processing " + this.className + '.' + methodNode.name
                                                      + methodNode.desc);
    }

    public boolean isAbstractMatch() {
        return (getMatchType() != null) && (!getMatchType().isExactMatch());
    }

    public String getClassName() {
        return this.className;
    }

    public MatchType getMatchType() {
        return this.weavedClassInfo.getMatchType();
    }

    public List<InnerClassNode> getInnerClasses() {
        return this.innerClasses;
    }

    public Map<Method, MergeMethodVisitor> getMethods() {
        return Collections.unmodifiableMap(this.methods);
    }

    public Map<Method, MethodNode> getMethodsToInline() {
        return this.methodsToInline;
    }

    public WeavedClassInfo getWeaveClassInfo() {
        return this.weavedClassInfo;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.bytes);
        result = 31 * result + (this.className == null ? 0 : this.className.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MixinClassVisitor other = (MixinClassVisitor) obj;
        if (this.className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!this.className.equals(other.className)) {
            return false;
        }
        if (!Arrays.equals(this.bytes, other.bytes)) {
            return false;
        }
        return true;
    }

    public void print() {
        TraceClassVisitor cv = new TraceClassVisitor(new PrintWriter(System.err));

        for (MergeMethodVisitor m : this.methods.values()) {
            m.instructions.accept(cv.visitMethod(m.access, m.name, m.desc, m.signature, null));
        }
        cv.visitEnd();
    }
}