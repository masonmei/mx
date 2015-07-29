package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

@PointCut
public class HandleInternalInvokerPointCut extends MethodInvokerPointCut {
    public HandleInternalInvokerPointCut(ClassTransformer classTransformer) {
        super(new InterfaceMatcher("org/springframework/web/servlet/HandlerAdapter"), OrMethodMatcher
                                                                                              .getMethodMatcher(new MethodMatcher[] {createExactMethodMatcher("invokeHandleMethod",
                                                                                                                                                                     new String[] {"(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Lorg/springframework/web/method/HandlerMethod;)Lorg/springframework/web/servlet/ModelAndView;"}),
                                                                                                                                            createExactMethodMatcher("invokeHandlerMethod",
                                                                                                                                                                            new String[] {"(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;Lorg/springframework/web/method/HandlerMethod;)Lorg/springframework/web/servlet/ModelAndView;"})}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object invoker, Object[] args) {
        String methodName = null;
        Class controller = null;
        StringBuilder tracerName = new StringBuilder("Spring/Java");
        try {
            HandlerMethod methodInfo = (HandlerMethod) args[2];

            methodName = methodInfo._nr_getBridgedMethod().getName();
            controller = methodInfo._nr_getBean().getClass();

            tracerName.append(getControllerName(methodName, controller));

            if (isNormalizeTransactions()) {
                setTransactionName(transaction, methodName, controller);
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, "Unabled to pull controller and method from spring framework.");
            Agent.LOG.log(Level.FINEST, "Exception grabbing spring controller.", e);
            tracerName.append(sig.getMethodName());
        }

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