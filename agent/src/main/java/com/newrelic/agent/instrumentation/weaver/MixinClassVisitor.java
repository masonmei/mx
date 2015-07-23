package com.newrelic.agent.instrumentation.weaver;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.api.agent.weaver.MatchType;

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
        super(Agent.ASM_LEVEL);
        this.bytes = bytes;
        this.instrumentationPackage = instrumentationPackage;
        this.weavedClassInfo = weavedClassInfo;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        this.interfaces = interfaces;
    }

    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        innerClasses.add(new InnerClassNode(name, outerName, innerName, access));
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        Method method = new Method(name, desc);
        if ((0x440 & access) != 0) {
            return null;
        }

        if ("<clinit>".equals(name)) {
            instrumentationPackage.getLogger().warning(className + " in " + instrumentationPackage.implementationTitle
                                                               + " contains a class constructor (static initializer)."
                                                               + "  This code will be discarded.");

            return null;
        }

        MergeMethodVisitor node =
                new MergeMethodVisitor(instrumentationPackage, className, Agent.ASM_LEVEL, access, name, desc, signature,
                                              exceptions);

        methods.put(method, node);

        return node;
    }

    public void visitEnd() {
        super.visitEnd();

        removeInitSuperCalls();

        List<Method> emptyMethods = Lists.newArrayList();
        for (Entry<Method, MergeMethodVisitor> entry : methods.entrySet()) {
            if ((entry.getValue()).instructions.size() == 0) {
                emptyMethods.add(entry.getKey());
            } else if ((entry.getValue()).isNewMethod()) {
                methodsToInline.put(entry.getKey(), entry.getValue());
            }

        }

        methods.keySet().removeAll(emptyMethods);
    }

    private void removeInitSuperCalls() {
        for (MergeMethodVisitor methodNode : methods.values()) {
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
                    instrumentationPackage.getLogger()
                            .finest("Keeping <init> method after encountering opcode " + node.getOpcode());

                    return;
                }
            }
        }
        instrumentationPackage.getLogger().finest("Discarding <init> method on weaved class " + className);
        methodNode.instructions.clear();
    }

    private void removeInitSuperCalls(MergeMethodVisitor methodNode) {
        AbstractInsnNode[] insnNodes = methodNode.instructions.toArray();
        for (AbstractInsnNode insn : insnNodes) {
            methodNode.instructions.remove(insn);

            if (insn.getOpcode() == 183) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
                if (methodInsnNode.owner.equals(weavedClassInfo.getSuperName())) {
                    return;
                }
            }
        }

        throw new IllegalInstructionException("Error processing " + className + '.' + methodNode.name
                                                      + methodNode.desc);
    }

    public boolean isAbstractMatch() {
        return (getMatchType() != null) && (!getMatchType().isExactMatch());
    }

    public String getClassName() {
        return className;
    }

    public MatchType getMatchType() {
        return weavedClassInfo.getMatchType();
    }

    public List<InnerClassNode> getInnerClasses() {
        return innerClasses;
    }

    public Map<Method, MergeMethodVisitor> getMethods() {
        return Collections.unmodifiableMap(methods);
    }

    public Map<Method, MethodNode> getMethodsToInline() {
        return methodsToInline;
    }

    public WeavedClassInfo getWeaveClassInfo() {
        return weavedClassInfo;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + Arrays.hashCode(bytes);
        result = 31 * result + (className == null ? 0 : className.hashCode());
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
        if (className == null) {
            if (other.className != null) {
                return false;
            }
        } else if (!className.equals(other.className)) {
            return false;
        }
        if (!Arrays.equals(bytes, other.bytes)) {
            return false;
        }
        return true;
    }

    public void print() {
        TraceClassVisitor cv = new TraceClassVisitor(new PrintWriter(System.err));

        for (MergeMethodVisitor m : methods.values()) {
            m.instructions.accept(cv.visitMethod(m.access, m.name, m.desc, m.signature, null));
        }
        cv.visitEnd();
    }
}