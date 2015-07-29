package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import java.lang.reflect.Method;

@PointCut
public class HandlerMethodInvokerPointCut extends MethodInvokerPointCut
{
  private static final String SPRING_2X_METHOD = "doInvokeMethod";
  private static final String SPRING_3X_METHOD = "resolveHandlerArguments";

  public HandlerMethodInvokerPointCut(ClassTransformer classTransformer)
  {
    super(new ExactClassMatcher("org/springframework/web/bind/annotation/support/HandlerMethodInvoker"), OrMethodMatcher.getMethodMatcher(new MethodMatcher[] { createExactMethodMatcher("doInvokeMethod", new String[] { "(Ljava/lang/reflect/Method;Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;" }), createExactMethodMatcher("resolveHandlerArguments", new String[] { "(Ljava/lang/reflect/Method;Ljava/lang/Object;Lorg/springframework/web/context/request/NativeWebRequest;Lorg/springframework/ui/ExtendedModelMap;)[Ljava/lang/Object;" }) }));
  }

  public Tracer doGetTracer(final Transaction transaction, ClassMethodSignature sig, Object invoker, Object[] args)
  {
    final String methodName = ((Method)args[0]).getName();
    final Class controller = args[1].getClass();

    if (isNormalizeTransactions()) {
      setTransactionName(transaction, methodName, controller);
    }

    if ("resolveHandlerArguments".equals(sig.getMethodName()))
    {
      return null;
    }
    return new DefaultTracer(transaction, sig, invoker, new SimpleMetricNameFormat("Spring/Java/" + controller.getName() + '/' + methodName))
    {
      protected void doFinish(Throwable throwable)
      {
        if (!HandlerMethodInvokerPointCut.this.isNormalizationDisabled())
          HandlerMethodInvokerPointCut.this.setTransactionName(transaction, methodName, controller);
      }
    };
  }
}