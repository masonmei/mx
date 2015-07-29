package com.newrelic.agent.instrumentation.pointcuts.jruby;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class NR_ONLY_JRubyTracerPointCut extends TracerFactoryPointCut
{
  private final int METRIC_NAME_ARGUMENT = 2;

  public NR_ONLY_JRubyTracerPointCut(ClassTransformer ct) {
    super(NR_ONLY_JRubyTracerPointCut.class, new ExactClassMatcher("com/newrelic/api/jruby/JavaAgentBackend"), new ExactMethodMatcher("trace", new String[0]));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    String metric = args[2].toString();
    return new DefaultTracer(transaction, sig, object, new SimpleMetricNameFormat(metric));
  }
}