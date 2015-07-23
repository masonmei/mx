//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class SpringPointCut extends TracerFactoryPointCut {
    public static final String SPRING_CONTROLLER = "SpringController";
    public static final String SPRING_VIEW = "SpringView";
    private static final String REDIRECT_VIEW_SYNTAX = "/redirect:";
    private static final String FORWARD_VIEW_SYNTAX = "/forward:";
    private static final Pattern HTTP_PATTERN = Pattern.compile("(.*)https?://.*");
    private final boolean normalizeTransactions = HandlerMethodInvokerPointCut
                                                          .useViewNameToNormalize(ServiceFactory.getConfigService()
                                                                                          .getDefaultAgentConfig());

    public SpringPointCut(ClassTransformer ct) {
        super(SpringPointCut.class, new InterfaceMatcher("org/springframework/web/servlet/HandlerAdapter"),
                     new ExactMethodMatcher("handle", "(Ljavax/servlet/http/HttpServletRequest;"
                                                              + "Ljavax/servlet/http/HttpServletResponse;"
                                                              + "Ljava/lang/Object;)"
                                                              + "Lorg/springframework/web/servlet/ModelAndView;"));
    }

    static String getModelAndViewViewName(Object modelAndView)
            throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException,
                           NoSuchMethodException {
        String viewName;
        if (modelAndView instanceof ModelAndView) {
            viewName = ((ModelAndView) modelAndView).getViewName();
        } else {
            viewName = (String) modelAndView.getClass().getMethod("getViewName", new Class[0])
                                        .invoke(modelAndView, new Object[0]);
        }

        return cleanModelAndViewName(viewName);
    }

    static String cleanModelAndViewName(String viewName) {
        if (viewName != null && viewName.length() != 0) {
            if (viewName.charAt(0) != 47) {
                viewName = '/' + viewName;
            }

            if (viewName.startsWith("/redirect:")) {
                return "/redirect:*";
            } else if (viewName.startsWith("/forward:")) {
                return null;
            } else {
                viewName = ServiceFactory.getNormalizationService().getUrlBeforeParameters(viewName);
                Matcher paramDelimiterMatcher = HTTP_PATTERN.matcher(viewName);
                if (paramDelimiterMatcher.matches()) {
                    viewName = paramDelimiterMatcher.group(1) + '*';
                }

                return viewName;
            }
        } else {
            return viewName;
        }
    }

    public Tracer doGetTracer(final Transaction transaction, final ClassMethodSignature sig, final Object controller,
                              Object[] args) {
        final Object handler = args[2];
        return new DefaultTracer(transaction, sig, controller) {
            protected void doFinish(int opcode, Object modelView) {
                if (modelView != null && SpringPointCut.this.normalizeTransactions) {
                    this.setTransactionName(transaction, modelView);
                }

                StringBuilder tracerName;
                String metricName;
                if (handler != null) {
                    tracerName = new StringBuilder("SpringController/");
                    tracerName.append(this.getControllerName(handler.getClass()));
                    metricName = tracerName.toString();
                } else {
                    tracerName = new StringBuilder("SpringController/");
                    tracerName.append(this.getControllerName(controller.getClass()));
                    tracerName.append('/').append(sig.getMethodName());
                    metricName = tracerName.toString();
                }

                this.setMetricNameFormat(new SimpleMetricNameFormat(metricName));
                super.doFinish(opcode, modelView);
            }

            private String getControllerName(Class<?> controllerx) {
                String controllerName = controllerx.getName();
                int indexOf = controllerName.indexOf("$$EnhancerBy");
                if (indexOf > 0) {
                    controllerName = controllerName.substring(0, indexOf);
                }

                return controllerName;
            }

            private void setTransactionName(Transaction transactionx, Object modelView) {
                if (transactionx.isTransactionNamingEnabled()) {
                    TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
                    if (policy.canSetTransactionName(transactionx, TransactionNamePriority.FRAMEWORK)) {
                        String modelAndViewName = this.doGetModelAndViewName(modelView);
                        if (modelAndViewName == null) {
                            return;
                        }

                        if (Agent.LOG.isLoggable(Level.FINER)) {
                            String msg = MessageFormat
                                                 .format("Setting transaction name to \"{0}\" using Spring ModelView",
                                                                new Object[] {modelAndViewName});
                            Agent.LOG.finer(msg);
                        }

                        policy.setTransactionName(transactionx, modelAndViewName, "SpringView",
                                                         TransactionNamePriority.FRAMEWORK);
                    }

                }
            }

            private String doGetModelAndViewName(Object modelAndView) {
                try {
                    return SpringPointCut.getModelAndViewViewName(modelAndView);
                } catch (Exception var3) {
                    Agent.LOG.log(Level.FINE, "Unable to parse Spring ModelView", var3);
                    return null;
                }
            }
        };
    }

    public boolean isEnabled() {
        return ((Boolean) ServiceFactory.getConfigService().getDefaultAgentConfig()
                                  .getProperty("enable_spring_tracing", Boolean.valueOf(true))).booleanValue();
    }
}
