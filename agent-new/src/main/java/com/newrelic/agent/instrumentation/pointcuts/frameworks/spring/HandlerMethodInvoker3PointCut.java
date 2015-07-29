package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.lang.reflect.Method;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class HandlerMethodInvoker3PointCut extends TracerFactoryPointCut {
    public HandlerMethodInvoker3PointCut(ClassTransformer classTransformer) {
        super(new PointCutConfiguration("spring_handler_method_invoker"),
                     new ExactClassMatcher("org/springframework/web/bind/annotation/support/HandlerMethodInvoker"),
                     createExactMethodMatcher("invokeHandlerMethod",
                                                     new String[] {"(Ljava/lang/reflect/Method;Ljava/lang/Object;"
                                                                           +
                                                                           "Lorg/springframework/web/context/request/NativeWebRequest;Lorg/springframework/ui/ExtendedModelMap;)Ljava/lang/Object;"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object invoker, Object[] args) {
        StringBuilder tracerName = new StringBuilder("Spring/Java");
        String methodName = ((Method) args[0]).getName();
        Class controller = args[1].getClass();

        tracerName.append(getControllerName(methodName, controller));
        return new DefaultTracer(transaction, sig, invoker, new SimpleMetricNameFormat(tracerName.toString()));
    }

    private String getControllerName(String methodName, Class<?> controller) {
        String controllerName = controller.getName();
        int indexOf = controllerName.indexOf("$$EnhancerBy");
        if (indexOf > 0) {
            controllerName = controllerName.substring(0, indexOf);
        }
        return '/' + controllerName + '/' + methodName;
    }
}