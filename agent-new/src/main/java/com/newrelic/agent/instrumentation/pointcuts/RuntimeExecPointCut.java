package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import java.lang.instrument.UnmodifiableClassException;
import java.util.logging.Level;

@PointCut
public class RuntimeExecPointCut extends TracerFactoryPointCut
{
  public RuntimeExecPointCut(ClassTransformer classTransformer)
  {
    super(new PointCutConfiguration(RuntimeExecPointCut.class.getName(), null, false), new ExactClassMatcher("java/lang/Runtime"), createExactMethodMatcher("exec", new String[] { "(Ljava/lang/String;[Ljava/lang/String;Ljava/io/File;)Ljava/lang/Process;", "([Ljava/lang/String;[Ljava/lang/String;Ljava/io/File;)Ljava/lang/Process;" }));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args)
  {
    return new DefaultTracer(transaction, sig, object, new ClassMethodMetricNameFormat(sig, object));
  }

  public void noticeTransformerStarted(ClassTransformer classTransformer)
  {
    InstrumentationProxy instrumentation = ServiceFactory.getAgent().getInstrumentation();
    if (instrumentation.isRetransformClassesSupported())
      try {
        instrumentation.retransformClasses(new Class[] { Runtime.class });
      } catch (UnmodifiableClassException e) {
        Agent.LOG.log(Level.FINER, "Unable to retransform java.lang.Runtime", e);
      }
  }
}