//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class QuartzSystemPointCut extends TracerFactoryPointCut {
  public QuartzSystemPointCut(ClassTransformer classTransformer) {
    super(new PointCutConfiguration("quartz_system"), ExactClassMatcher
                                                              .or(new String[] {"org/quartz/impl/jdbcjobstore"
                                                                                        + "/JobStoreSupport",
                                                                                       "org/quartz/simpl/RAMJobStore"}),
                 createExactMethodMatcher("acquireNextTrigger",
                                                 new String[] {"(Lorg/quartz/core/SchedulingContext;)V",
                                                                      "(Lorg/quartz/core/SchedulingContext;J)"
                                                                              + "Lorg/quartz/Trigger;"}));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object job, Object[] args) {
    return new QuartzSystemPointCut.QuartzJobTracer(transaction, sig, job);
  }

  protected boolean isDispatcher() {
    return true;
  }

  private static class QuartzJobTracer extends OtherRootTracer {
    public QuartzJobTracer(Transaction transaction, ClassMethodSignature sig, Object job) {
      super(transaction, sig, job, new ClassMethodMetricNameFormat(sig, job, "OtherTransaction/Job"));
      transaction.setIgnore(true);
    }
  }
}
