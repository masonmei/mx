//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.frameworks.struts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class XWork2ResultPC extends TracerFactoryPointCut {
  public XWork2ResultPC(ClassTransformer classTransformer) {
    super(XWork2ResultPC.class, new InterfaceMatcher("com/opensymphony/xwork2/Result"),
                 createExactMethodMatcher("execute",
                                                 new String[] {"(Lcom/opensymphony/xwork2/ActionInvocation;)V"}));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object result, Object[] args) {
    String name;
    try {
      Object t;
      if (args[0] instanceof ActionInvocation) {
        t = ((ActionInvocation) args[0]).getAction();
      } else {
        t = args[0].getClass().getMethod("getAction", new Class[0]).invoke(args[0], new Object[0]);
      }

      name = t.getClass().getName();
    } catch (Throwable var7) {
      name = "Unknown";
    }

    return new DefaultTracer(transaction, sig, result, new SimpleMetricNameFormat("StrutsResult/" + name));
  }
}
