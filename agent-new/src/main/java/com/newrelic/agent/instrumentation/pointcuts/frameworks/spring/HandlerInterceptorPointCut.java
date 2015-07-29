package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class HandlerInterceptorPointCut extends TracerFactoryPointCut
{
  public HandlerInterceptorPointCut(ClassTransformer classTransformer)
  {
    super(HandlerInterceptorPointCut.class, new InterfaceMatcher("org/springframework/web/servlet/HandlerInterceptor"), createMethodMatcher(new MethodMatcher[] { new ExactMethodMatcher("preHandle", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;)Z"), new ExactMethodMatcher("postHandle", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Lorg/springframework/web/servlet/ModelAndView;)V"), new ExactMethodMatcher("afterCompletion", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Ljava/lang/Object;Ljava/lang/Exception;)V") }));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object intercepter, Object[] args)
  {
    return new DefaultTracer(transaction, sig, intercepter, new SimpleMetricNameFormat("Spring/HandlerInterceptor", ClassMethodMetricNameFormat.getMetricName(sig, intercepter, "Spring/Java")));
  }
}