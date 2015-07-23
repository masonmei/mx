package com.newrelic.agent.tracers.metricname;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.util.Strings;

public class ClassMethodMetricNameFormat extends AbstractMetricNameFormat {
    private final ClassMethodSignature signature;
    private final String className;
    private final String prefix;
    private String metricName;

    public ClassMethodMetricNameFormat(ClassMethodSignature sig, Object object) {
        this(sig, object, "Java");
    }

    public ClassMethodMetricNameFormat(ClassMethodSignature sig, Object object, String prefix) {
        signature = sig;
        className = (object == null ? sig.getClassName() : object.getClass().getName());
        this.prefix = prefix;
    }

    public static String getMetricName(ClassMethodSignature sig, Object object) {
        return getMetricName(sig, object, "Java");
    }

    public static String getMetricName(ClassMethodSignature sig, Object object, String prefix) {
        if (object == null) {
            return getMetricName(sig, prefix);
        }
        return Strings.join('/', new String[] {prefix, object.getClass().getName(), sig.getMethodName()});
    }

    public static String getMetricName(ClassMethodSignature sig, String prefix) {
        String className = sig.getClassName().replaceAll("/", ".");
        return Strings.join('/', new String[] {prefix, className, sig.getMethodName()});
    }

    public String getMetricName() {
        if (metricName == null) {
            metricName = Strings.join('/', new String[] {prefix, className, signature.getMethodName()});
        }
        return metricName;
    }
}