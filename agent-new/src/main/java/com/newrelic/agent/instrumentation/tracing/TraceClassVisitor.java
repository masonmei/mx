package com.newrelic.agent.instrumentation.tracing;

import java.util.Set;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.Label;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.deps.com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.TraceInformation;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;

public class TraceClassVisitor extends ClassVisitor {
  private final String className;
  private final InstrumentationContext instrumentationContext;
  private final TraceInformation traceInfo;
  private final Set<Method> tracedMethods = Sets.newHashSet();

  public TraceClassVisitor(ClassVisitor cv, String className, InstrumentationContext context) {
    super(Agent.ASM_LEVEL, cv);

    this.className = className;
    instrumentationContext = context;
    traceInfo = context.getTraceInformation();
  }

  public void visitEnd() {
    super.visitEnd();

    if (!traceInfo.getTraceAnnotations().isEmpty()) {
      Agent.LOG.finer("Traced " + className + " methods " + tracedMethods);
      if (tracedMethods.size() != traceInfo.getTraceAnnotations().size()) {
        Set expected = Sets.newHashSet(traceInfo.getTraceAnnotations().keySet());
        expected.removeAll(tracedMethods);

        Agent.LOG.finer("While tracing " + className + " the following methods were not traced: " + expected);
      }
    }
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    Method method = new Method(name, desc);

    if (traceInfo.getIgnoreTransactionMethods().contains(method)) {
      instrumentationContext.markAsModified();
      return new AdviceAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
        protected void onMethodEnter() {
          BridgeUtils.getCurrentTransaction(this);

          ((Transaction) BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true).build()).ignore();
        }
      };
    }

    final TraceDetails trace = (TraceDetails) traceInfo.getTraceAnnotations().get(method);

    if (null != trace) {
      tracedMethods.add(method);
      PointCut pointCut = instrumentationContext.getOldStylePointCut(method);
      if (pointCut == null) {
        boolean custom = trace.isCustom();

        if ((trace.excludeFromTransactionTrace()) && (trace.isLeaf())) {
          mv = new FlyweightTraceMethodVisitor(className, mv, access, name, desc, trace,
                                                      instrumentationContext.getClassBeingRedefined());
        } else {
          mv = new TraceMethodVisitor(className, mv, access, name, desc, trace, custom,
                                             instrumentationContext.getClassBeingRedefined());

          if (!trace.getParameterAttributeNames().isEmpty()) {
            for (ParameterAttributeName attr : trace.getParameterAttributeNames()) {
              final ParameterAttributeName param = attr;
              if (param.getMethodMatcher().matches(access, name, desc, null)) {
                try {
                  final Type type = method.getArgumentTypes()[param.getIndex()];
                  if (type.getSort() == 9) {
                    Agent.LOG.log(Level.FINE,
                                         "Unable to record an attribute value for {0}.{1} because"
                                                 + " it is an array",
                                         new Object[] {className, method});
                  } else {
                    mv = new AdviceAdapter(262144, mv, access, name, desc) {
                      protected void onMethodEnter() {
                        super.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "publicApi",
                                               BridgeUtils.PUBLIC_API_TYPE);

                        PublicApi api = (PublicApi) BytecodeGenProxyBuilder
                                                            .newBuilder(PublicApi.class, this,
                                                                               false).build();

                        push(param.getAttributeName());
                        loadArg(param.getIndex());

                        if (type.getSort() != 10) {
                          box(type);
                        }

                        dup();

                        instanceOf(Type.getType(Number.class));
                        Label objectLabel = newLabel();
                        Label skipLabel = newLabel();
                        ifZCmp(153, objectLabel);

                        checkCast(Type.getType(Number.class));
                        api.addCustomParameter("", Integer.valueOf(0));

                        goTo(skipLabel);
                        visitLabel(objectLabel);

                        invokeVirtual(Type.getType(Object.class),
                                             new Method("toString", Type.getType(String.class),
                                                               new Type[0]));

                        api.addCustomParameter("", "");

                        visitLabel(skipLabel);
                      }
                    };
                  }
                } catch (ArrayIndexOutOfBoundsException e) {
                  Agent.LOG.log(Level.FINEST, e, e.toString(), new Object[0]);
                }
              }
            }

          }

          if (trace.rollupMetricName().length > 0) {
            final int cacheId = AgentBridge.instrumentation.addToObjectCache(trace.rollupMetricName());
            mv = new AdviceAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
              protected void onMethodEnter() {
                getStatic(BridgeUtils.TRACED_METHOD_TYPE, "CURRENT", BridgeUtils.TRACED_METHOD_TYPE);

                super.getStatic(BridgeUtils.AGENT_BRIDGE_TYPE, "instrumentation",
                                       BridgeUtils.INSTRUMENTATION_TYPE);

                Instrumentation instrumentation = (Instrumentation) BytecodeGenProxyBuilder
                                                                            .newBuilder(Instrumentation.class,
                                                                                               this,
                                                                                               true)
                                                                            .build();

                instrumentation.getCachedObject(cacheId);

                super.checkCast(Type.getType(String.class));

                TracedMethod tracedMethod = (TracedMethod) BytecodeGenProxyBuilder
                                                                   .newBuilder(TracedMethod.class, this,
                                                                                      false).build();

                tracedMethod.setRollupMetricNames((String[]) null);
              }
            };
          }

          if (TransactionName.isSimpleTransactionName(trace.transactionName())) {
            mv = new AdviceAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
              protected void onMethodEnter() {
                TracedMethod tracedMethod = (TracedMethod) BytecodeGenProxyBuilder
                                                                   .newBuilder(TracedMethod.class, this,
                                                                                      true).build();

                getStatic(BridgeUtils.TRACED_METHOD_TYPE, "CURRENT", BridgeUtils.TRACED_METHOD_TYPE);

                tracedMethod.nameTransaction(trace.transactionName().transactionNamePriority);
              }
            };
          } else if (trace.transactionName() != null) {
            mv = new AdviceAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
              protected void onMethodEnter() {
                BridgeUtils.getCurrentTransaction(this);

                Transaction transaction =
                        (Transaction) BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true)
                                              .build();

                TransactionName transactionName = trace.transactionName();
                transaction.setTransactionName(transactionName.transactionNamePriority,
                                                      transactionName.override,
                                                      transactionName.category,
                                                      new String[] {transactionName.path});

                pop();
              }
            };
          }

          if (trace.isWebTransaction()) {
            mv = new AdviceAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
              protected void onMethodExit(int opcode) {
                getStatic(BridgeUtils.TRANSACTION_TYPE, "CURRENT", BridgeUtils.TRANSACTION_TYPE);

                ((Transaction) BytecodeGenProxyBuilder.newBuilder(Transaction.class, this, true)
                                       .build()).convertToWebTransaction();
              }
            };
          }
        }

        instrumentationContext.addTimedMethods(new Method[] {method});
      } else {
        Agent.LOG.warning(className + '.' + method + " is matched to trace, but it was already instrumented by "
                                  + pointCut.toString());
      }

    }

    if (traceInfo.getIgnoreApdexMethods().contains(method)) {
      instrumentationContext.markAsModified();
      mv = new AdviceAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
        protected void onMethodEnter() {
          invokeStatic(BridgeUtils.NEW_RELIC_API_TYPE, TraceMethodVisitor.IGNORE_APDEX_METHOD);
        }

      };
    }

    return mv;
  }
}