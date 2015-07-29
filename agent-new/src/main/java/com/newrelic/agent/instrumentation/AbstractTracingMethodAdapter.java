package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.objectweb.asm.Label;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.agent.logging.IAgentLogger;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.logging.Level;

abstract class AbstractTracingMethodAdapter extends AdviceAdapter
{
  private static final String JAVA_LANG_THROWABLE = "java/lang/Throwable";
  private static final boolean sDebugTracers = false;
  protected final String methodName;
  private int tracerLocalId;
  private final Label startFinallyLabel = new Label();
  protected final GenericClassAdapter genericClassAdapter;
  private int invocationHandlerIndex = -1;
  protected final MethodBuilder methodBuilder;

  public AbstractTracingMethodAdapter(GenericClassAdapter genericClassAdapter, MethodVisitor mv, int access, Method method)
  {
    super(327680, mv, access, method.getName(), method.getDescriptor());
    this.genericClassAdapter = genericClassAdapter;
    this.methodName = method.getName();
    this.methodBuilder = new MethodBuilder(this, access);
  }

  String getMethodDescriptor() {
    return this.methodDesc;
  }

  protected void systemOutPrint(String message)
  {
    systemPrint(message, false);
  }

  protected void systemPrint(String message, boolean error) {
    getStatic(Type.getType(System.class), error ? "err" : "out", Type.getType(PrintStream.class));
    visitLdcInsn(message);
    invokeVirtual(Type.getType(PrintStream.class), new Method("println", "(Ljava/lang/String;)V"));
  }

  protected void onMethodEnter()
  {
    int methodIndex = this.genericClassAdapter.addInstrumentedMethod(this);
    if (this.genericClassAdapter.canModifyClassStructure()) {
      setInvocationFieldIndex(methodIndex);
    }

    try
    {
      Type tracerType = getTracerType();
      this.tracerLocalId = newLocal(tracerType);

      visitInsn(1);
      storeLocal(this.tracerLocalId);

      Label startLabel = new Label();
      Label endLabel = new Label();
      Label exceptionLabel = new Label();

      this.mv.visitTryCatchBlock(startLabel, endLabel, exceptionLabel, "java/lang/Throwable");
      this.mv.visitLabel(startLabel);
      loadGetTracerArguments();
      invokeGetTracer();

      storeLocal(this.tracerLocalId);
      this.mv.visitLabel(endLabel);
      Label doneLabel = new Label();
      goTo(doneLabel);
      this.mv.visitLabel(exceptionLabel);
      if (Agent.LOG.isLoggable(Level.FINER))
      {
        this.mv.visitMethodInsn(182, "java/lang/Throwable", "printStackTrace", "()V", false);
        systemPrint(MessageFormat.format("An error occurred creating a tracer for {0}.{1}{2}", new Object[] { this.genericClassAdapter.className, this.methodName, this.methodDesc }), true);
      }
      else
      {
        int exceptionVar = newLocal(Type.getType(Throwable.class));
        visitVarInsn(58, exceptionVar);
      }
      this.mv.visitLabel(doneLabel);
    }
    catch (Throwable e) {
      Agent.LOG.severe(MessageFormat.format("An error occurred transforming {0}.{1}{2} : {3}", new Object[] { this.genericClassAdapter.className, this.methodName, this.methodDesc, e.toString() }));

      throw new RuntimeException(e);
    }
  }

  private void setInvocationFieldIndex(int id) {
    this.invocationHandlerIndex = id;
  }

  public int getInvocationHandlerIndex() {
    return this.invocationHandlerIndex;
  }

  protected final Type getTracerType() {
    return MethodBuilder.INVOCATION_HANDLER_TYPE;
  }

  protected final void invokeGetTracer() {
    this.methodBuilder.invokeInvocationHandlerInterface(false);
  }

  protected abstract void loadGetTracerArguments();

  public GenericClassAdapter getGenericClassAdapter() {
    return this.genericClassAdapter;
  }

  public void visitCode()
  {
    super.visitCode();
    super.visitLabel(this.startFinallyLabel);
  }

  public void visitMaxs(int maxStack, int maxLocals)
  {
    Label endFinallyLabel = new Label();
    super.visitTryCatchBlock(this.startFinallyLabel, endFinallyLabel, endFinallyLabel, "java/lang/Throwable");
    super.visitLabel(endFinallyLabel);
    onFinally(191);
    super.visitInsn(191);
    super.visitMaxs(maxStack, maxLocals);
  }

  protected void onMethodExit(int opcode)
  {
    if (opcode != 191)
      onFinally(opcode);
  }

  protected void onFinally(int opcode)
  {
    Label end = new Label();
    if (opcode == 191) {
      if ("<init>".equals(this.methodName))
      {
        return;
      }
      dup();
      int exceptionVar = newLocal(Type.getType(Throwable.class));
      visitVarInsn(58, exceptionVar);

      loadLocal(this.tracerLocalId);
      ifNull(end);
      loadLocal(this.tracerLocalId);

      checkCast(MethodBuilder.INVOCATION_HANDLER_TYPE);

      invokeTraceFinishWithThrowable(exceptionVar);
    } else {
      Object loadReturnValue = null;
      if (opcode != 177) {
        loadReturnValue = new StoreReturnValueAndReload(opcode);
      }

      loadLocal(this.tracerLocalId);
      ifNull(end);
      loadLocal(this.tracerLocalId);

      invokeTraceFinish(opcode, loadReturnValue);
    }
    visitLabel(end);
  }

  protected final void invokeTraceFinish(int opcode, Object loadReturnValue)
  {
    this.methodBuilder.loadSuccessful().loadArray(Object.class, new Object[] { Integer.valueOf(opcode), loadReturnValue }).invokeInvocationHandlerInterface(true);
  }

  protected final void invokeTraceFinishWithThrowable(final int exceptionVar) {
    this.methodBuilder.loadUnsuccessful().loadArray(Object.class, new Object[] { new Runnable()
    {
      public void run() {
        AbstractTracingMethodAdapter.this.visitVarInsn(25, exceptionVar);
      }
    }
     }).invokeInvocationHandlerInterface(true);
  }

  private final class StoreReturnValueAndReload
    implements Runnable
  {
    private final int returnVar;

    public StoreReturnValueAndReload(int opcode)
    {
      Type returnType = Type.getReturnType(AbstractTracingMethodAdapter.this.methodDesc);

      if (returnType.getSize() == 2)
        AbstractTracingMethodAdapter.this.dup2();
      else {
        AbstractTracingMethodAdapter.this.dup();
      }
      returnType = AbstractTracingMethodAdapter.this.methodBuilder.box(returnType);

      this.returnVar = AbstractTracingMethodAdapter.this.newLocal(returnType);
      AbstractTracingMethodAdapter.this.storeLocal(this.returnVar, returnType);
    }

    public void run()
    {
      AbstractTracingMethodAdapter.this.loadLocal(this.returnVar);
    }
  }
}