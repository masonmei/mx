package com.newrelic.agent.instrumentation.pointcuts.frameworks.spring;

import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.deps.com.google.common.collect.Maps;

public abstract class MethodInvokerPointCut extends TracerFactoryPointCut {
    protected static final String TO_REMOVE = "$$EnhancerBy";
    private static final String TRANSACTION_NAMING_CONFIG_PARAMETER_NAME = "transaction_naming_scheme";
    private static final String SPRING_FRAMEWORK_CONFIG_PARAMETER_NAME = "spring_framework";
    private static final String CONTROLLER_METHOD_NAMING = "controller_method";
    private static final String VIEW_NAMING = "view";
    private static final String DEFAULT_NAMING_METHOD = "controller_method";
    private final boolean useFullPackageName;
    private final boolean normalizeTransactions;
    private final boolean normalizationDisabled;

    public MethodInvokerPointCut(ClassMatcher classMatcher, MethodMatcher methodMatcher) {
        super(new PointCutConfiguration("spring_handler_method_invoker"), classMatcher, methodMatcher);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        useFullPackageName =
                ((Boolean) getSpringConfiguration(config).getProperty("use_full_package_name", Boolean.valueOf(false)))
                        .booleanValue();
        normalizeTransactions = "controller_method".equals(getSpringConfiguration(config)
                                                                   .getProperty("transaction_naming_scheme",
                                                                                       "controller_method"));

        normalizationDisabled = ((!normalizeTransactions) && (!useViewNameToNormalize(config)));
    }

    private static BaseConfig getSpringConfiguration(AgentConfig config) {
        Map props = (Map) config.getInstrumentationConfig().getProperty("spring_framework", Maps.newHashMap());

        return new BaseConfig(props);
    }

    static boolean useViewNameToNormalize(AgentConfig config) {
        return "view".equals(getSpringConfiguration(config)
                                     .getProperty("transaction_naming_scheme", "controller_method"));
    }

    protected boolean isNormalizeTransactions() {
        return normalizeTransactions;
    }

    protected boolean isNormalizationDisabled() {
        return normalizationDisabled;
    }

    protected boolean isUseFullPackageName() {
        return useFullPackageName;
    }

    protected void setTransactionName(Transaction transaction, String methodName, Class<?> pController) {
        if (!transaction.isTransactionNamingEnabled()) {
            return;
        }

        TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
        if (policy.canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK)) {
            String controller = getControllerName(methodName, pController);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Spring controller",
                                                         new Object[] {controller});

                Agent.LOG.finer(msg);
            }
            policy.setTransactionName(transaction, controller, "SpringController", TransactionNamePriority.FRAMEWORK);
        }
    }

    private String getControllerName(String methodName, Class<?> controller) {
        String controllerName = isUseFullPackageName() ? controller.getName() : controller.getSimpleName();
        int indexOf = controllerName.indexOf("$$EnhancerBy");
        if (indexOf > 0) {
            controllerName = controllerName.substring(0, indexOf);
        }
        return '/' + controllerName + '/' + methodName;
    }
}