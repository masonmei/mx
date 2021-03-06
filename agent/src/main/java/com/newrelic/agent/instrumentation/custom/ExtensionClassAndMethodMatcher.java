package com.newrelic.agent.instrumentation.custom;

import java.util.List;

import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.TraceClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.DelegatingTraceDetails;
import com.newrelic.agent.instrumentation.tracing.ParameterAttributeName;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.util.Strings;

public class ExtensionClassAndMethodMatcher implements TraceClassAndMethodMatcher {
    private final ClassMatcher classMatcher;
    private final MethodMatcher methodMatcher;
    private final TraceDetails traceDetails;

    public ExtensionClassAndMethodMatcher(Extension extension, Pointcut pointcut, String metricPrefix,
                                          ClassMatcher classMatcher, MethodMatcher methodMatcher, boolean custom,
                                          List<ParameterAttributeName> reportedParams, InstrumentationType instType) {
        String metricName = pointcut.getMetricNameFormat();

        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;

        boolean webTransaction = false;
        if ("web".equals(pointcut.getTransactionType())) {
            webTransaction = true;
        }

        traceDetails = TraceDetailsBuilder.newBuilder().setMetricName(metricName)
                               .setMetricPrefix(getMetricPrefix(metricName, metricPrefix))
                               .setNameTransaction(pointcut.getNameTransaction() != null)
                               .setIgnoreTransaction(pointcut.isIgnoreTransaction())
                               .setExcludeFromTransactionTrace(pointcut.isExcludeFromTransactionTrace())
                               .setDispatcher(pointcut.isTransactionStartPoint()).setCustom(custom)
                               .setWebTransaction(webTransaction)
                               .setInstrumentationSourceName(pointcut.getClass().getName())
                               .setInstrumentationType(instType).setInstrumentationSourceName(extension.getName())
                               .setParameterAttributeNames(reportedParams).build();
    }

    public ExtensionClassAndMethodMatcher(String extensioName, String metricName, String metricPrefix,
                                          ClassMatcher classMatcher, MethodMatcher methodMatcher, boolean dispatcher,
                                          boolean skipTransTrace, boolean ignoreTrans, String tracerFactoryName) {
        this.classMatcher = classMatcher;
        this.methodMatcher = methodMatcher;

        traceDetails = TraceDetailsBuilder.newBuilder().setMetricName(metricName)
                               .setMetricPrefix(getMetricPrefix(metricName, metricPrefix)).setDispatcher(dispatcher)
                               .setExcludeFromTransactionTrace(skipTransTrace).setIgnoreTransaction(ignoreTrans)
                               .setInstrumentationSourceName(extensioName)
                               .setInstrumentationType(InstrumentationType.CustomYaml)
                               .setTracerFactoryName(tracerFactoryName).setCustom(true).build();
    }

    private String getMetricPrefix(String metricName, String metricPrefix) {
        if (metricName != null) {
            return null;
        }
        return metricPrefix;
    }

    public ClassMatcher getClassMatcher() {
        return classMatcher;
    }

    public MethodMatcher getMethodMatcher() {
        return methodMatcher;
    }

    public TraceDetails getTraceDetails() {
        final String metricName = traceDetails.metricName();
        final String metricPrefix = traceDetails.metricPrefix();
        return new DelegatingTraceDetails(traceDetails) {
            public String getFullMetricName(String pClassName, String pMethodName) {
                if ((metricPrefix == null) && (metricName == null)) {
                    return null;
                }
                if (metricPrefix == null) {
                    return getStringWhenMetricPrefixNull();
                }
                if (metricName == null) {
                    return Strings.join('/', new String[] {metricPrefix, "${className}", pMethodName});
                }

                return Strings.join('/', new String[] {metricPrefix, metricName});
            }

            private String getStringWhenMetricPrefixNull() {
                if (dispatcher()) {
                    return metricName;
                }
                if (metricName.startsWith("OtherTransaction")) {
                    return metricName;
                }
                if (metricName.startsWith("/")) {
                    return "OtherTransaction" + metricName;
                }
                return Strings.join('/', new String[] {"OtherTransaction", metricName});
            }
        };
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (classMatcher == null ? 0 : classMatcher.hashCode());
        result = 31 * result + (methodMatcher == null ? 0 : methodMatcher.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ExtensionClassAndMethodMatcher other = (ExtensionClassAndMethodMatcher) obj;
        if (classMatcher == null) {
            if (other.classMatcher != null) {
                return false;
            }
        } else if (!classMatcher.equals(other.classMatcher)) {
            return false;
        }
        if (methodMatcher == null) {
            if (other.methodMatcher != null) {
                return false;
            }
        } else if (!methodMatcher.equals(other.methodMatcher)) {
            return false;
        }
        return true;
    }
}