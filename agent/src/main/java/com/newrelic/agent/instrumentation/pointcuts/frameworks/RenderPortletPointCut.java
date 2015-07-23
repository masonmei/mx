package com.newrelic.agent.instrumentation.pointcuts.frameworks;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class RenderPortletPointCut extends AbstractPortletPointCut {
    public RenderPortletPointCut(ClassTransformer classTransformer) {
        super(RenderPortletPointCut.class, createExactMethodMatcher("render",
                                                                           new String[] {"(Ljavax/portlet/RenderRequest;Ljavax/portlet/RenderResponse;)V"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object portlet, Object[] args) {
        return new DefaultTracer(transaction, sig, portlet, new ClassMethodMetricNameFormat(sig, portlet, "Portlet"));
    }
}