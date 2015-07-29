package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassNameFilter;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class ProcessPointCut extends TracerFactoryPointCut
{
  public static final String UNIXPROCESS_CLASS_NAME = "java/lang/UNIXProcess";
  public static final String PROCESS_IMPL_CLASS_NAME = "java/lang/ProcessImpl";

  public ProcessPointCut(ClassTransformer classTransformer)
  {
    super(ProcessPointCut.class, ExactClassMatcher.or(new String[] { "java/lang/ProcessImpl", "java/lang/UNIXProcess" }), createExactMethodMatcher("waitFor", new String[] { "()I" }));

    classTransformer.getClassNameFilter().addIncludeClass("java/lang/ProcessImpl");
    classTransformer.getClassNameFilter().addIncludeClass("java/lang/UNIXProcess");
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    return new DefaultTracer(transaction, sig, object, new ClassMethodMetricNameFormat(sig, object));
  }
}