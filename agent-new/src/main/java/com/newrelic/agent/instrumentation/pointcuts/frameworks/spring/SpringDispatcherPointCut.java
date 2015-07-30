package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class SpringDispatcherPointCut extends TracerFactoryPointCut {
    static final String DISPATCHER_SERVLET_CLASS_NAME = "org/springframework/web/servlet/DispatcherServlet";
    private static final String RENDER_METHOD_NAME = "render";
    private final boolean normalizeTransactions;

    public SpringDispatcherPointCut(ClassTransformer classTransformer) {
        super(SpringDispatcherPointCut.class,
                     new ExactClassMatcher("org/springframework/web/servlet/DispatcherServlet"),
                     createMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("render",
                                                                                            "(Lorg/springframework/web/servlet/ModelAndView;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V"),
                                                                     new ExactMethodMatcher("doDispatch",
                                                                                                   "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V")}));

        this.normalizeTransactions = HandlerMethodInvokerPointCut
                                             .useViewNameToNormalize(ServiceFactory.getConfigService()
                                                                             .getDefaultAgentConfig());
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object dispatcher, Object[] args) {
        if ("render" == sig.getMethodName()) {
            StringBuilder metricName = new StringBuilder("SpringView");
            if (canSetTransactionName(transaction)) {
                try {
                    String viewName = SpringPointCut.getModelAndViewViewName(args[0]);
                    if (viewName != null) {
                        metricName.append(viewName);
                        if (this.normalizeTransactions) {
                            setTransactionName(transaction, viewName);
                        }
                    }
                } catch (Exception e) {
                    metricName.append("/Java/").append(dispatcher.getClass().getName()).append('/')
                            .append(sig.getMethodName());
                }
            } else {
                metricName.append("/Java/").append(dispatcher.getClass().getName()).append('/')
                        .append(sig.getMethodName());
            }

            return new DefaultTracer(transaction, sig, dispatcher, new SimpleMetricNameFormat(metricName.toString()));
        }
        return new DefaultTracer(transaction, sig, dispatcher, new ClassMethodMetricNameFormat(sig, dispatcher));
    }

    private boolean canSetTransactionName(Transaction transaction) {
        return TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy()
                       .canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK);
    }

    private void setTransactionName(Transaction transaction, String viewName) {
        if (!transaction.isTransactionNamingEnabled()) {
            return;
        }
        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if ((Agent.LOG.isLoggable(Level.FINER)) && (policy.canSetTransactionName(transaction,
                                                                                        TransactionNamePriority
                                                                                                .FRAMEWORK))) {
            String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Spring view",
                                                     new Object[] {viewName});
            Agent.LOG.finer(msg);
        }

        policy.setTransactionName(transaction, viewName, "SpringView", TransactionNamePriority.FRAMEWORK);
    }
}