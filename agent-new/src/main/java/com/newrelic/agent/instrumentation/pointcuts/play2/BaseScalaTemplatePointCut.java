package com.newrelic.agent.instrumentation.pointcuts.play2;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NameMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class BaseScalaTemplatePointCut extends TracerFactoryPointCut
{
  private static final boolean DEFAULT_ENABLED = false;
  private static final String POINT_CUT_NAME = BaseScalaTemplatePointCut.class.getName();

  public BaseScalaTemplatePointCut(ClassTransformer classTransformer) {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
  }

  private static PointCutConfiguration createPointCutConfig() {
    return new PointCutConfiguration(POINT_CUT_NAME, "play2_instrumentation", false);
  }

  private static ClassMatcher createClassMatcher()
  {
    return new ChildClassMatcher("play.templates.BaseScalaTemplate");
  }

  private static MethodMatcher createMethodMatcher() {
    return new NameMethodMatcher("apply");
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    if (!transaction.isStarted()) {
      Transaction.clearTransaction();
      return null;
    }
    return new DefaultTracer(transaction, sig, object, new ClassMethodMetricNameFormat(sig, object));
  }
}