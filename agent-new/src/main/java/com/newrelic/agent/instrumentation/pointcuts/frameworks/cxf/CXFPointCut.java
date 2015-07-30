package com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class CXFPointCut extends TracerFactoryPointCut {
    static final String CXF_ENDPOINT_ADDRESS_PARAMETER_NAME = "cfx_end_point";

    public CXFPointCut(ClassTransformer classTransformer) {
        super(CXFPointCut.class, new ExactClassMatcher("org/apache/cxf/transport/servlet/ServletDestination"),
                     createExactMethodMatcher("invoke", new String[] {"(Ljavax/servlet/ServletConfig;"
                                                                              + "Ljavax/servlet/ServletContext;"
                                                                              +
                                                                              "Ljavax/servlet/http/HttpServletRequest;"
                                                                              +
                                                                              "Ljavax/servlet/http/HttpServletResponse;)"
                                                                              + "V"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object servletDest, Object[] args) {
        try {
            Object endpointInfo = servletDest.getClass().getMethod("getEndpointInfo", new Class[0])
                                          .invoke(servletDest, new Object[0]);
            Object address =
                    endpointInfo.getClass().getMethod("getAddress", new Class[0]).invoke(endpointInfo, new Object[0]);

            transaction.getInternalParameters().put("cfx_end_point", address);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, "Error parsing CXF transaction", t);
        }
        return new DefaultTracer(transaction, sig, servletDest, new ClassMethodMetricNameFormat(sig, servletDest));
    }
}