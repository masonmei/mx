package com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf;

import java.lang.reflect.Method;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.util.Strings;

@PointCut
public class ClientProxyPointCut extends TracerFactoryPointCut {
    public ClientProxyPointCut(ClassTransformer classTransformer) {
        super(ClientProxyPointCut.class, new ExactClassMatcher("org/apache/cxf/frontend/ClientProxy"),
                     createExactMethodMatcher("invokeSync",
                                                     new String[] {"(Ljava/lang/reflect/Method;"
                                                                           +
                                                                           "Lorg/apache/cxf/service/model/BindingOperationInfo;[Ljava/lang/Object;)Ljava/lang/Object;"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object proxy, Object[] args) {
        Method method = (Method) args[0];

        return new DefaultTracer(transaction, sig, proxy, new SimpleMetricNameFormat(Strings.join('/',
                                                                                                         new String[] {"Java",
                                                                                                                              method.getDeclaringClass()
                                                                                                                                      .getName(),
                                                                                                                              method.getName()})));
    }
}