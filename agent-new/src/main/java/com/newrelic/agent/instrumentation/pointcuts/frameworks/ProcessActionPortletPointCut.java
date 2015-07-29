package com.newrelic.agent.instrumentation.pointcuts.frameworks;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class ProcessActionPortletPointCut extends AbstractPortletPointCut {
    public ProcessActionPortletPointCut(ClassTransformer classTransformer) {
        super(ProcessActionPortletPointCut.class, createExactMethodMatcher("processAction",
                                                                                  new String[] {"(Ljavax/portlet/ActionRequest;Ljavax/portlet/ActionResponse;)V"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object portlet, Object[] args) {
        return new DefaultTracer(transaction, sig, portlet, new ClassMethodMetricNameFormat(sig, portlet, "Portlet"));
    }
}