package com.newrelic.agent.instrumentation.pointcuts.frameworks.jersey;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class JerseyResourcePointCut extends TracerFactoryPointCut {
    public JerseyResourcePointCut(ClassTransformer transformer) {
        super(new PointCutConfiguration(JerseyResourcePointCut.class), ExactClassMatcher
                                                                               .or(new String[]
                                                                                           {"com/sun/jersey/server/impl/model/method/dispatch/ResourceJavaMethodDispatcher",
                                                                                                        "com/sun/jersey/impl/model/method/dispatch/ResourceJavaMethodDispatcher"}),
                     createExactMethodMatcher("dispatch",
                                                     new String[] {"(Ljava/lang/Object;"
                                                                           + "Lcom/sun/jersey/api/core/HttpContext;)"
                                                                           + "V"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher, Object[] args) {
        try {
            Class dispatcherClass = dispatcher.getClass().getClassLoader().loadClass(sig.getClassName());

            Field methodField = dispatcherClass.getDeclaredField("method");
            methodField.setAccessible(true);
            Method method = (Method) methodField.get(dispatcher);
            String methodName = method.getName();
            ClassMethodMetricNameFormat metricNameFormatter =
                    new ClassMethodMetricNameFormat(new ClassMethodSignature(args[0].getClass().getName(), methodName,
                                                                                    ""), args[0]);

            return new DefaultTracer(transaction, sig, dispatcher, metricNameFormatter);
        } catch (Exception e) {
            Agent.LOG.log(Level.FINER, "Jersey resource error", e);
        }
        return null;
    }
}