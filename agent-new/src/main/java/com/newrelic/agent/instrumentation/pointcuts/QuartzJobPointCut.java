//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts;

import java.text.MessageFormat;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.OtherRootTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class QuartzJobPointCut extends TracerFactoryPointCut {
    public QuartzJobPointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration("quartz_job"), new InterfaceMatcher("org/quartz/Job"),
                     createExactMethodMatcher("execute", new String[] {"(Lorg/quartz/JobExecutionContext;)V"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object job, Object[] args) {
        return new QuartzJobPointCut.QuartzJobTracer(transaction, sig, job, args[0]);
    }

    protected boolean isDispatcher() {
        return true;
    }

    private static class QuartzJobTracer extends OtherRootTracer {
        public QuartzJobTracer(Transaction transaction, ClassMethodSignature sig, Object job, Object context) {
            super(transaction, sig, job, new ClassMethodMetricNameFormat(sig, job, "OtherTransaction/Job"));

            try {
                Object e = context.getClass().getMethod("getJobDetail", new Class[0]).invoke(context, new Object[0]);
                this.setAttribute("name", e.getClass().getMethod("getFullName", new Class[0]).invoke(e, new Object[0]));
            } catch (Throwable var6) {
                Agent.LOG.finer(MessageFormat.format("An error occurred getting a Quartz job name",
                                                            new Object[] {var6.toString()}));
            }

            if (Agent.isDebugEnabled()) {
                Agent.LOG.fine("Quartz job started");
            }

        }
    }
}
