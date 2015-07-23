//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.google.common.collect.Lists;
import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.asm.Utils;

class MergeMethodVisitor extends MethodNode {
    private static final Method NOTICE_INSTRUMENTATION_ERROR_METHOD;

    static {
        NOTICE_INSTRUMENTATION_ERROR_METHOD = new Method("noticeInstrumentationError", Type.VOID_TYPE,
                                                                new Type[] {Type.getType(Throwable.class),
                                                                                   Type.getType(String.class)});
    }

    protected final int firstLocal;
    private final Method method;
    private final String className;
    private final InstrumentationPackage instrumentationPackage;
    private int nextLocalIndex;
    private MethodInsnNode invokeOriginalNode;

    public MergeMethodVisitor(InstrumentationPackage instrumentationPackage, String className, int api, int access,
                              String name, String desc, String signature, String[] exceptions) {
        super(api, access, name, desc, signature, exceptions);
        this.instrumentationPackage = instrumentationPackage;
        this.method = new Method(name, desc);
        this.className = className;
        Type[] args = Type.getArgumentTypes(desc);
        int nextLocal = (8 & access) == 0 ? 1 : 0;

        for (int i = 0; i < args.length; ++i) {
            nextLocal += args[i].getSize();
        }

        this.firstLocal = nextLocal;
    }

    public static boolean isInitMethod(String name) {
        return "<init>".equals(name);
    }

    public static boolean isOriginalMethodInvocation(String owner, String name, String desc) {
        return owner.equals(BridgeUtils.WEAVER_TYPE.getInternalName()) && name.equals(WeaveUtils.CALL_ORIGINAL_METHOD
                                                                                              .getName())
                       && desc.equals(WeaveUtils.CALL_ORIGINAL_METHOD.getDescriptor());
    }

    public void visitEnd() {
        try {
            int e = this.determineIfNew();
            if (this.invokeOriginalNode != null) {
                boolean isVoid = this.method.getReturnType().equals(Type.VOID_TYPE);
                Label startOfOriginalMethod = new Label();
                Label endOfOriginalMethod = new Label();
                Object insertPoint;
                if (e >= 0) {
                    insertPoint = this.instructions.get(e);
                } else {
                    insertPoint = this.invokeOriginalNode;
                }

                boolean callsThrow = this.isThrowCalled();
                int rethrowExceptionIndex = -1;
                int returnLocalIndex = -1;
                if (!isVoid || callsThrow) {
                    Label startOfOriginalMethodLabelNode = new Label();
                    Label isStatic = new Label();
                    this.instructions.insert(this.getLabelNode(startOfOriginalMethodLabelNode));
                    this.instructions.add(this.getLabelNode(isStatic));
                    if (callsThrow) {
                        rethrowExceptionIndex = this.nextLocalIndex;
                        this.visitLocalVariable("rethrowException", Type.getDescriptor(Throwable.class), (String) null,
                                                       startOfOriginalMethodLabelNode, isStatic, rethrowExceptionIndex);
                    }

                    if (!isVoid) {
                        returnLocalIndex = this.nextLocalIndex;
                        this.visitLocalVariable("originalReturnValue", this.method.getReturnType().getDescriptor(),
                                                       (String) null, startOfOriginalMethodLabelNode, isStatic,
                                                       returnLocalIndex);
                    }
                }

                if (rethrowExceptionIndex >= 0) {
                    this.storeExceptionAtThrowSites(rethrowExceptionIndex);
                }

                LabelNode var19 = this.getLabelNode(startOfOriginalMethod);
                this.instructions.insertBefore((AbstractInsnNode) insertPoint, var19);
                boolean var20 = (this.access & 8) != 0;
                if (!var20) {
                    this.instructions.insertBefore((AbstractInsnNode) insertPoint, new VarInsnNode(25, 0));
                }

                int index = var20 ? 0 : 1;

                for (int afterOriginalMethodExceptionHandler = 0;
                     afterOriginalMethodExceptionHandler < this.method.getArgumentTypes().length;
                     ++afterOriginalMethodExceptionHandler) {
                    Type generator = this.method.getArgumentTypes()[afterOriginalMethodExceptionHandler];
                    this.instructions.insertBefore((AbstractInsnNode) insertPoint,
                                                          new VarInsnNode(generator.getOpcode(21), index));
                    index += generator.getSize();
                }

                this.instructions.insertBefore((AbstractInsnNode) insertPoint,
                                                      new MethodInsnNode(var20 ? 184 : 182, this.className,
                                                                                this.method.getName(),
                                                                                this.method.getDescriptor()));
                if (returnLocalIndex >= 0) {
                    this.instructions.insertBefore((AbstractInsnNode) insertPoint,
                                                          new VarInsnNode(this.method.getReturnType().getOpcode(54),
                                                                                 returnLocalIndex));
                }

                this.instructions.insertBefore((AbstractInsnNode) insertPoint, this.getLabelNode(endOfOriginalMethod));
                if (returnLocalIndex >= 0) {
                    Object var21 = e >= 0 ? this.invokeOriginalNode : insertPoint;
                    this.instructions.insertBefore((AbstractInsnNode) var21,
                                                          new VarInsnNode(this.method.getReturnType().getOpcode(21),
                                                                                 returnLocalIndex));
                }

                AbstractInsnNode var22;
                if (returnLocalIndex < 0) {
                    var22 = this.invokeOriginalNode.getNext();
                    if (var22.getOpcode() == 87) {
                        this.instructions.remove(var22);
                    } else {
                        this.instrumentationPackage.getLogger()
                                .severe("Unexpected instruction " + var22.getOpcode() + ", Method: " + this.className
                                                + '.' + this.method + ", expected " + 87);
                    }
                } else if (this.method.getReturnType().getSort() != 10 && this.method.getReturnType().getSort() != 9) {
                    var22 = this.invokeOriginalNode.getNext();
                    if (var22.getOpcode() == 87) {
                        if (this.method.getReturnType().getSize() == 2) {
                            this.instructions.insertBefore(var22, new InsnNode(88));
                            this.instructions.remove(var22);
                        }
                    } else {
                        if (var22.getOpcode() == 192) {
                            this.instructions.remove(var22);
                        } else {
                            this.expectCastOrInvokeWithObject(var22);
                        }

                        AbstractInsnNode var23 = this.invokeOriginalNode.getNext();
                        if (var23.getOpcode() == 182) {
                            this.instructions.remove(var23);
                        } else {
                            this.instrumentationPackage.getLogger()
                                    .severe("Unexpected instruction " + var22.getOpcode() + ", Method: "
                                                    + this.className + '.' + this.method + ", expected " + 182);
                        }
                    }
                } else if (!Type.getType(Object.class).equals(this.method.getReturnType())) {
                    var22 = this.invokeOriginalNode.getNext();
                    if (var22.getOpcode() != 87 && var22.getOpcode() != 176) {
                        this.expectCastOrInvokeWithObject(var22);
                    }
                }

                this.instructions.remove(this.invokeOriginalNode);
                Label var25 = new Label();
                this.visitLabel(var25);
                GeneratorAdapter var24 = new GeneratorAdapter(this.access, this.method, this);
                Label start;
                if (rethrowExceptionIndex >= 0) {
                    start = new Label();
                    var24.dup();
                    this.visitVarInsn(25, rethrowExceptionIndex);
                    this.visitJumpInsn(166, start);
                    this.visitInsn(191);
                    this.visitLabel(start);
                }

                this.noticeInstrumentationErrorInstructions(var24);
                if (returnLocalIndex >= 0) {
                    this.visitVarInsn(this.method.getReturnType().getOpcode(21), returnLocalIndex);
                }

                this.visitInsn(this.method.getReturnType().getOpcode(172));
                this.visitTryCatchBlock(endOfOriginalMethod, var25, var25, Type.getInternalName(Throwable.class));
                start = new Label();
                this.instructions.insert(this.getLabelNode(start));
                this.initializePreambleLocals(var19);
                MethodNode preambleHandler = new MethodNode(Agent.ASM_LEVEL);
                Label beforeOriginalMethodExceptionHandler = new Label();
                var24 = new GeneratorAdapter(this.access, this.method, preambleHandler);
                var24.goTo(startOfOriginalMethod);
                var24.visitLabel(beforeOriginalMethodExceptionHandler);
                this.noticeInstrumentationErrorInstructions(var24);
                this.instructions.insertBefore(var19, preambleHandler.instructions);
                this.visitTryCatchBlock(start, startOfOriginalMethod, beforeOriginalMethodExceptionHandler,
                                               Type.getInternalName(Throwable.class));
            }

        } catch (IllegalInstructionException var17) {
            this.instrumentationPackage.getLogger().severe("Unable to process method " + this.method);
            throw var17;
        } catch (Exception var18) {
            this.instrumentationPackage.getLogger().severe("Unable to process method " + this.method);
            throw new RuntimeException(var18);
        }
    }

    private void storeExceptionAtThrowSites(int rethrowExceptionIndex) {
        AbstractInsnNode[] arr$ = this.instructions.toArray();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            AbstractInsnNode insnNode = arr$[i$];
            if (191 == insnNode.getOpcode()) {
                this.instructions.insertBefore(insnNode, new VarInsnNode(58, rethrowExceptionIndex));
                this.instructions.insertBefore(insnNode, new VarInsnNode(25, rethrowExceptionIndex));
            }
        }

    }

    private boolean isThrowCalled() {
        AbstractInsnNode[] arr$ = this.instructions.toArray();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            AbstractInsnNode insnNode = arr$[i$];
            if (191 == insnNode.getOpcode()) {
                return true;
            }
        }

        return false;
    }

    private void expectCastOrInvokeWithObject(AbstractInsnNode nextInstruction) {
        if (nextInstruction.getOpcode() == 192) {
            this.instructions.remove(nextInstruction);
        } else if (nextInstruction instanceof MethodInsnNode) {
            MethodInsnNode methodNode = (MethodInsnNode) nextInstruction;
            Type type = Type.getType(methodNode.desc);
            if (type.getArgumentTypes().length <= 0 || !Type.getType(Object.class).equals(type.getArgumentTypes()[0])) {
                this.instrumentationPackage.getLogger()
                        .severe("Unexpected instruction " + nextInstruction.getOpcode() + ", Method: "
                                        + methodNode.owner + '.' + methodNode.name + methodNode.desc);
            }
        } else {
            this.instrumentationPackage.getLogger().severe("Unexpected instruction " + nextInstruction.getOpcode());
        }

    }

    private void initializePreambleLocals(LabelNode startOfOriginalMethodLabelNode) {
        List localsInPreamble = this.getLocalsInPreamble(startOfOriginalMethodLabelNode);
        if (!localsInPreamble.isEmpty()) {
            int firstLocalIndex = Utils.getFirstLocal(this.access, this.method);
            LabelNode localsStart = this.getLabelNode(new Label());
            this.instructions.insert(localsStart);
            Iterator i$ = localsInPreamble.iterator();

            while (i$.hasNext()) {
                LocalVariableNode local = (LocalVariableNode) i$.next();
                if (local.index >= firstLocalIndex) {
                    this.changeLocalVariableScopeStart(local, localsStart);
                }
            }
        }

    }

    private void changeLocalVariableScopeStart(LocalVariableNode local, LabelNode newStart) {
        Type type = Type.getType(local.desc);
        local.start = newStart;
        List collidingLocals = this.getCollidingVariables(local, this.localVariables);
        if (!collidingLocals.isEmpty()) {
            this.instrumentationPackage.getLogger().log(Level.FINEST, "slot {0} ({1}) collision detected",
                                                               new Object[] {Integer.valueOf(local.index), local.name});
            int initialValue = this.nextLocalIndex;
            this.nextLocalIndex += type.getSize();
            Iterator i$ = collidingLocals.iterator();

            while (i$.hasNext()) {
                LocalVariableNode collidingLocal = (LocalVariableNode) i$.next();
                this.instrumentationPackage.getLogger()
                        .log(Level.FINEST, "\tchanging {0}", new Object[] {collidingLocal.name});
                collidingLocal.index = initialValue;
                this.changeLocalSlot(local.index, initialValue, collidingLocal.start, collidingLocal.end);
            }
        }

        AbstractInsnNode initialValue1 = this.getInitialValueInstruction(type);
        if (initialValue1 != null) {
            this.instructions.insert(newStart, new VarInsnNode(type.getOpcode(54), local.index));
            this.instructions.insert(newStart, initialValue1);
        }

    }

    private List<LocalVariableNode> getCollidingVariables(LocalVariableNode local,
                                                          List<LocalVariableNode> otherLocals) {
        ArrayList collisions = new ArrayList();
        Iterator i$ = otherLocals.iterator();

        while (true) {
            LocalVariableNode otherLocal;
            do {
                if (!i$.hasNext()) {
                    return collisions;
                }

                otherLocal = (LocalVariableNode) i$.next();
            } while (local.name.equals(otherLocal.name) && local.desc.equals(otherLocal.desc));

            if (this.shareSlot(local, otherLocal) && this.scopesOverlap(local, otherLocal)) {
                collisions.add(otherLocal);
            }
        }
    }

    private boolean shareSlot(LocalVariableNode local, LocalVariableNode otherLocal) {
        return local.index == otherLocal.index;
    }

    private boolean scopesOverlap(LocalVariableNode local, LocalVariableNode otherLocal) {
        return this.scopeContainsAnyPartOf(local, otherLocal) || this.scopeContainsAnyPartOf(otherLocal, local);
    }

    private boolean scopeContainsAnyPartOf(LocalVariableNode local, LocalVariableNode otherLocal) {
        for (Object currentNode = local.start; currentNode != null && !currentNode.equals(local.end);
             currentNode = ((AbstractInsnNode) currentNode).getNext()) {
            if (currentNode.equals(otherLocal.start)) {
                return true;
            }

            if (currentNode.equals(otherLocal.end)) {
                return !currentNode.equals(local.start);
            }
        }

        return false;
    }

    private void changeLocalSlot(int oldSlot, int newSlot, LabelNode start, LabelNode end) {
        for (Object currentNode = null == start.getPrevious() ? start : start.getPrevious();
             null != currentNode && !currentNode.equals(end);
             currentNode = ((AbstractInsnNode) currentNode).getNext()) {
            if (((AbstractInsnNode) currentNode).getType() == 2) {
                VarInsnNode currentInsn = (VarInsnNode) currentNode;
                if (currentInsn.var == oldSlot) {
                    currentInsn.var = newSlot;
                }
            }
        }

    }

    private AbstractInsnNode getInitialValueInstruction(Type type) {
        switch (type.getSort()) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return new InsnNode(3);
            case 6:
                return new InsnNode(11);
            case 7:
                return new InsnNode(9);
            case 8:
                return new InsnNode(14);
            case 9:
            case 10:
                return new InsnNode(1);
            default:
                return null;
        }
    }

    private List<LocalVariableNode> getLocalsInPreamble(AbstractInsnNode insertPoint) {
        int endIndex = this.instructions.indexOf(insertPoint);
        if (endIndex < 0) {
            return Collections.emptyList();
        } else {
            ArrayList locals = Lists.newArrayList();
            Iterator i$ = this.localVariables.iterator();

            while (i$.hasNext()) {
                LocalVariableNode local = (LocalVariableNode) i$.next();
                int startIndex = this.instructions.indexOf(local.start);
                int end = this.instructions.indexOf(local.end);
                if (startIndex < endIndex && end > endIndex) {
                    locals.add(local);
                }
            }

            return locals;
        }
    }

    private void noticeInstrumentationErrorInstructions(GeneratorAdapter generator) {
        generator.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "instrumentation", BridgeUtils.INSTRUMENTATION_TYPE);
        generator.swap();
        generator.push(this.instrumentationPackage.implementationTitle);
        generator.invokeInterface(BridgeUtils.INSTRUMENTATION_TYPE, NOTICE_INSTRUMENTATION_ERROR_METHOD);
    }

    private int determineIfNew() {
        AnalyzerAdapter stackAnalyzer =
                new AnalyzerAdapter(this.className, this.access, this.name, this.desc, new MethodVisitor(Agent.ASM_LEVEL) {
                });
        boolean callsThrow = false;
        int lastStackZeroIndex = 0;
        AbstractInsnNode[] inst = this.instructions.toArray();

        for (int i = 0; i < inst.length; ++i) {
            int stackSize = stackAnalyzer.stack == null ? 0 : stackAnalyzer.stack.size();
            if (stackSize == 0) {
                lastStackZeroIndex = i;
            }

            inst[i].accept(stackAnalyzer);
            if (inst[i].getOpcode() == 191) {
                callsThrow = true;
            }

            if (inst[i] instanceof MethodInsnNode) {
                MethodInsnNode invoke = (MethodInsnNode) inst[i];
                if (isOriginalMethodInvocation(invoke.owner, invoke.name, invoke.desc)) {
                    if (callsThrow) {
                        throw new IllegalInstructionException(this.className + '.' + this.name + this.desc
                                                                      + " can only throw an exception from the "
                                                                      + "original method invocation");
                    }

                    this.invokeOriginalNode = invoke;
                    if (stackSize > 0) {
                        return lastStackZeroIndex;
                    }

                    return -1;
                }
            }
        }

        return -1;
    }

    public boolean isNewMethod() {
        return this.invokeOriginalNode == null && !isInitMethod(this.name);
    }

    public String getClassName() {
        return this.className;
    }

    public Method getMethod() {
        return this.method;
    }

    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, index);
        if (index >= this.nextLocalIndex) {
            this.nextLocalIndex = index + Type.getType(desc).getSize();
        }

    }
}
