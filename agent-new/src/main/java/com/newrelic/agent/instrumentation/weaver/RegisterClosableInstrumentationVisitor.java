package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import java.io.Closeable;

public class RegisterClosableInstrumentationVisitor extends ClassVisitor
{
  private final InstrumentationPackage instrumentationPackage;

  public RegisterClosableInstrumentationVisitor(InstrumentationPackage instrumentationPackage, ClassVisitor cv)
  {
    super(327680, cv);
    this.instrumentationPackage = instrumentationPackage;
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
  {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    return new AdviceAdapter(327680, mv, access, name, desc)
    {
      public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        if ((BridgeUtils.PRIVATE_API_TYPE.getInternalName().equals(owner)) && ("addSampler".equals(name)))
        {
          dup();

          getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "instrumentation", BridgeUtils.INSTRUMENTATION_TYPE);

          swap();

          push(RegisterClosableInstrumentationVisitor.this.instrumentationPackage.implementationTitle);

          swap();

          BytecodeGenProxyBuilder builder = BytecodeGenProxyBuilder.newBuilder(Instrumentation.class, this, false);
          ((Instrumentation)builder.build()).registerCloseable("ImplementationTitle", (Closeable)null);
        }
      }
    };
  }
}