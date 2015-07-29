//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.context;

import java.util.HashSet;
import java.util.Set;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AsyncApi;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;

public class CurrentTransactionRewriter {
  public CurrentTransactionRewriter() {
  }

  public static ClassVisitor rewriteCurrentTransactionReferences(final ClassVisitor cv, ClassReader reader) {
    final Set localTransactionMethods = getLocalTransactionMethods(reader);
    return localTransactionMethods.isEmpty() ? cv : new ClassVisitor(Agent.ASM_LEVEL, cv) {
      public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                       String[] exceptions) {
        Object mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (localTransactionMethods.contains(new Method(name, desc))) {
          mv = new CurrentTransactionRewriter.RewriteVisitor((MethodVisitor) mv, access, name, desc);
        }

        return (MethodVisitor) mv;
      }
    };
  }

  private static boolean isCurrentTransactionReference(int opcode, String owner, String name) {
    return 178 == opcode && BridgeUtils.isTransactionType(owner) && "CURRENT".equals(name);
  }

  private static boolean isCurrentTransactionMethod(String owner, String name) {
    return BridgeUtils.isAgentType(owner) && "getTransaction".equals(name);
  }

  private static boolean isAsyncApiMethod(String owner, String name) {
    return Type.getInternalName(AsyncApi.class).equals(owner) && "resumeAsync".equals(name);
  }

  private static Set<Method> getLocalTransactionMethods(ClassReader reader) {
    final HashSet methods = Sets.newHashSet();
    ClassVisitor cv = new ClassVisitor(Agent.ASM_LEVEL) {
      public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc,
                                       String signature, String[] exceptions) {
        return new MethodVisitor(Agent.ASM_LEVEL) {
          public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (CurrentTransactionRewriter.isCurrentTransactionReference(opcode, owner, name)) {
              methods.add(new Method(methodName, methodDesc));
            }

          }

          public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (CurrentTransactionRewriter.isAsyncApiMethod(owner, name) || CurrentTransactionRewriter
                                                                                    .isCurrentTransactionMethod(owner,
                                                                                                                       name)) {
              methods.add(new Method(methodName, methodDesc));
            }

          }
        };
      }
    };
    reader.accept(cv, 6);
    return methods;
  }

  private static class RewriteVisitor extends AdviceAdapter {
    final int transactionLocal;

    protected RewriteVisitor(MethodVisitor mv, int access, String name, String desc) {
      super(Agent.ASM_LEVEL, mv, access, name, desc);
      this.transactionLocal = this.newLocal(BridgeUtils.TRANSACTION_TYPE);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (CurrentTransactionRewriter.isCurrentTransactionReference(opcode, owner, name)) {
        this.loadLocal(this.transactionLocal);
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }

    }

    protected void onMethodEnter() {
      super.onMethodEnter();
      this.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "instrumentation", BridgeUtils.INSTRUMENTATION_TYPE);
      ((Instrumentation) BytecodeGenProxyBuilder.newBuilder(Instrumentation.class, this, false).build())
              .getTransaction();
      this.storeLocal(this.transactionLocal, BridgeUtils.TRANSACTION_TYPE);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      if (CurrentTransactionRewriter.isCurrentTransactionMethod(owner, name)) {
        this.pop();
        this.loadLocal(this.transactionLocal);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        if (CurrentTransactionRewriter.isAsyncApiMethod(owner, name)) {
          this.dup();
          this.storeLocal(this.transactionLocal);
        }
      }

    }
  }
}
