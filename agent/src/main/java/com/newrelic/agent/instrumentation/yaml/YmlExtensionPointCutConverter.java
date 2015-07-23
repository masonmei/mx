//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.yaml;

import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TracerFactoryException;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.api.agent.MethodTracerFactory;

public class YmlExtensionPointCutConverter {
    public static final String CLASS_MATCHER_KEY = "class_matcher";
    public static final String METHOD_MATCHER_KEY = "method_matcher";
    public static final String DISPATCHER_KEY = "dispatcher";
    public static final String METRIC_NAME_FORMAT_KEY = "metric_name_format";
    public static final String SKIP_TRANS_KEY = "skip_transaction_trace";
    public static final String IGNORE_TRANS_KEY = "ignore_transaction";
    public static final String TRACER_FACTOR_KEY = "tracer_factory";

    public YmlExtensionPointCutConverter() {
    }

    public static ExtensionClassAndMethodMatcher createExtensionPointCut(Map attrs, String defaultMetricPrefix,
                                                                         ClassLoader classLoader, String extName) {
        ClassMatcher classMatcher = getClassMatcher(attrs);
        MethodMatcher methodMatcher = getMethodMatcher(attrs);
        boolean dispatcher = getDispatcher(attrs);
        BaseConfig newConfig = new BaseConfig(attrs);
        boolean skipTransTrace =
                ((Boolean) newConfig.getProperty("skip_transaction_trace", Boolean.FALSE)).booleanValue();
        boolean ignoreTrans = ((Boolean) newConfig.getProperty("ignore_transaction", Boolean.FALSE)).booleanValue();
        Object format = attrs.get("metric_name_format");
        String metricName;
        if (format instanceof String) {
            metricName = format.toString();
        } else if (null == format) {
            metricName = null;
        } else {
            if (!(format instanceof MetricNameFormatFactory)) {
                throw new RuntimeException(MessageFormat.format("Unsupported {0} value",
                                                                       new Object[] {"metric_name_format"}));
            }

            Agent.LOG.log(Level.WARNING, MessageFormat
                                                 .format("The object property {0} is no longer supported in the agent"
                                                                 + ". The default naming mechanism will be used.",
                                                                new Object[] {"metric_name_format"}));
            metricName = null;
        }

        String tracerFactoryNameString =
                getTracerFactoryName(attrs, defaultMetricPrefix, dispatcher, format, classLoader);
        String nameOfExtension = extName == null ? "Unknown" : extName;
        return new ExtensionClassAndMethodMatcher(nameOfExtension, metricName, defaultMetricPrefix, classMatcher,
                                                         methodMatcher, dispatcher, skipTransTrace, ignoreTrans,
                                                         tracerFactoryNameString);
    }

    private static ClassMatcher getClassMatcher(Map attrs) {
        ClassMatcher classMatcher = PointCutFactory.getClassMatcher(attrs.get("class_matcher"));
        if (classMatcher == null) {
            throw new RuntimeException("No class matcher for " + attrs.toString());
        } else {
            return classMatcher;
        }
    }

    private static MethodMatcher getMethodMatcher(Map attrs) {
        MethodMatcher methodMatcher = PointCutFactory.getMethodMatcher(attrs.get("method_matcher"));
        if (methodMatcher == null) {
            throw new RuntimeException("No method matcher for " + attrs.toString());
        } else {
            return methodMatcher;
        }
    }

    private static boolean getDispatcher(Map attrs) {
        Object dispatcherProp = attrs.get("dispatcher");
        return dispatcherProp != null && Boolean.parseBoolean(dispatcherProp.toString());
    }

    private static String getTracerFactoryName(Map attrs, String prefix, boolean dispatcher, Object metricNameFormat,
                                               ClassLoader loader) {
        String tracerFactoryNameString = null;
        Object tracerFactoryName = attrs.get("tracer_factory");
        if (tracerFactoryName != null) {
            try {
                TracerFactory ex = getTracerFactory(tracerFactoryName.toString(), loader,
                                                           new TracerFactoryConfiguration(prefix, dispatcher,
                                                                                                 metricNameFormat,
                                                                                                 attrs));
                tracerFactoryNameString = tracerFactoryName.toString();
                ServiceFactory.getTracerService().registerTracerFactory(tracerFactoryNameString, ex);
            } catch (TracerFactoryException var8) {
                throw new RuntimeException("Unable to create tracer factory " + tracerFactoryName, var8);
            }
        }

        return tracerFactoryNameString;
    }

    public static TracerFactory getTracerFactory(String tracerFactoryName, ClassLoader classLoader,
                                                 TracerFactoryConfiguration config) throws TracerFactoryException {
        try {
            Class ex = classLoader.loadClass(tracerFactoryName);
            String msg =
                    MessageFormat.format("Instantiating custom tracer factory {0}", new Object[] {tracerFactoryName});
            Agent.LOG.finest(msg);
            if (TracerFactory.class.isAssignableFrom(ex)) {
                return instantiateTracerFactory(ex, config);
            } else if (MethodTracerFactory.class.isAssignableFrom(ex)) {
                return instantiateMethodTracerFactory(ex);
            } else {
                throw new TracerFactoryException("Unknown tracer factory type:" + tracerFactoryName);
            }
        } catch (Exception var5) {
            throw new TracerFactoryException("Unable to load tracer factory " + tracerFactoryName, var5);
        }
    }

    private static TracerFactory instantiateMethodTracerFactory(Class clazz) throws Exception {
        MethodTracerFactory factory = (MethodTracerFactory) clazz.newInstance();
        return new CustomTracerFactory(factory);
    }

    private static TracerFactory instantiateTracerFactory(Class<? extends TracerFactory> clazz,
                                                          TracerFactoryConfiguration config)
            throws TracerFactoryException {
        try {
            return (TracerFactory) clazz.getConstructor(new Class[] {TracerFactoryConfiguration.class})
                                           .newInstance(new Object[] {config});
        } catch (Exception var4) {
            try {
                return (TracerFactory) clazz.getConstructor(new Class[0]).newInstance(new Object[0]);
            } catch (Exception var3) {
                throw new TracerFactoryException("Unable to instantiate tracer factory " + clazz.getName(), var3);
            }
        }
    }
}
