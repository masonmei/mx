package com.newrelic.agent.instrumentation.yaml;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.ConfigurationConstruct;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NoMethodsMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Strings;

public class PointCutFactory {
    private final String defaultMetricPrefix;
    private final ClassLoader classLoader;
    private final String extensionName;

    public PointCutFactory(ClassLoader classLoader, String metricPrefix, String name) {
        this.classLoader = classLoader;
        defaultMetricPrefix = metricPrefix;
        extensionName = name;
    }

    static Collection<ClassMatcher> getClassMatchers(Collection matchers) {
        Collection list = new ArrayList(matchers.size());
        for (Iterator i$ = matchers.iterator(); i$.hasNext(); ) {
            Object matcher = i$.next();
            list.add(getClassMatcher(matcher));
        }
        return list;
    }

    static ClassMatcher getClassMatcher(Object yaml) {
        if ((yaml instanceof ClassMatcher)) {
            return (ClassMatcher) yaml;
        }
        if ((yaml instanceof String)) {
            return new ExactClassMatcher(((String) yaml).trim());
        }
        if ((yaml instanceof List)) {
            List list = (List) yaml;
            return OrClassMatcher.getClassMatcher(getClassMatchers(list));
        }
        return null;
    }

    static Collection<MethodMatcher> getMethodMatchers(Collection matchers) {
        Collection list = new ArrayList(matchers.size());
        for (Iterator i$ = matchers.iterator(); i$.hasNext(); ) {
            Object matcher = i$.next();
            list.add(getMethodMatcher(matcher));
        }
        return list;
    }

    static MethodMatcher getMethodMatcher(Object yaml) {
        MethodMatcher matcher = null;
        if ((yaml instanceof MethodMatcher)) {
            matcher = (MethodMatcher) yaml;
        } else {
            if ((yaml instanceof List)) {
                List list = (List) yaml;
                if ((!list.isEmpty()) && ((list.get(0) instanceof String)) && (list.get(0).toString().indexOf('(')
                                                                                       < 0)) {
                    return createExactMethodMatcher(list.get(0).toString().trim(),
                                                           Strings.trim(list.subList(1, list.size())));
                }

                return OrMethodMatcher.getMethodMatcher(getMethodMatchers(list));
            }
            if ((yaml instanceof String)) {
                String text = yaml.toString().trim();
                int index = text.indexOf('(');
                if (index > 0) {
                    String methodName = text.substring(0, index);
                    String methodDesc = text.substring(index);
                    return createExactMethodMatcher(methodName, methodDesc);
                }
                return new ExactMethodMatcher(text, new String[0]);
            }
        }
        return matcher;
    }

    public static ClassMethodSignature parseClassMethodSignature(String signature) {
        int methodArgIndex = signature.indexOf('(');
        if (methodArgIndex > 0) {
            String methodDesc = signature.substring(methodArgIndex);
            String classAndMethod = signature.substring(0, methodArgIndex);
            int methodStart = classAndMethod.lastIndexOf('.');
            if (methodStart > 0) {
                String methodName = classAndMethod.substring(methodStart + 1);
                String className = classAndMethod.substring(0, methodStart).replace('/', '.');
                return new ClassMethodSignature(className, methodName, methodDesc);
            }
        }
        return null;
    }

    public static MethodMatcher createExactMethodMatcher(String methodName, String methodDesc) {
        ExactMethodMatcher methodMatcher = new ExactMethodMatcher(methodName, methodDesc);
        return validateMethodMatcher(methodMatcher);
    }

    public static MethodMatcher createExactMethodMatcher(String methodName, Collection<String> methodDescriptions) {
        ExactMethodMatcher methodMatcher = new ExactMethodMatcher(methodName, methodDescriptions);
        return validateMethodMatcher(methodMatcher);
    }

    private static MethodMatcher validateMethodMatcher(ExactMethodMatcher methodMatcher) {
        try {
            methodMatcher.validate();
            return methodMatcher;
        } catch (InvalidMethodDescriptor e) {
            Agent.LOG.log(Level.SEVERE, MessageFormat
                                                .format("The method matcher can not be created, meaning the methods "
                                                                + "associated with it will not be monitored - {0}",
                                                               new Object[] {e.toString()}));

            Agent.LOG.log(Level.FINER, "Error creating method matcher.", e);
        }
        return new NoMethodsMatcher();
    }

    public static Collection<ConfigurationConstruct> getConstructs() {
        return new InstrumentationConstructor().constructs;
    }

    public Collection<ExtensionClassAndMethodMatcher> getPointCuts(Object config) throws ParseException {
        if ((config instanceof List)) {
            return getPointCuts((List) config);
        }
        if ((config instanceof Map)) {
            return getPointCuts((Map) config);
        }

        return Collections.EMPTY_LIST;
    }

    public ExtensionClassAndMethodMatcher getPointCut(Object obj) throws ParseException {
        if ((obj instanceof String)) {
            return getPointCut((String) obj);
        }
        if ((obj instanceof Map)) {
            return getPointCut((Map) obj);
        }
        throw new RuntimeException(MessageFormat.format("Unknown pointcut type: {0} ({1}",
                                                               new Object[] {obj, obj.getClass().getName()}));
    }

    public ExtensionClassAndMethodMatcher getPointCut(String string) throws ParseException {
        ClassMethodSignature sig = parseClassMethodSignature(string);
        if (sig != null) {
            return new ExtensionClassAndMethodMatcher(extensionName, null, defaultMetricPrefix,
                                                             new ExactClassMatcher(sig.getClassName()),
                                                             createExactMethodMatcher(sig.getMethodName(),
                                                                                             sig.getMethodDesc()),
                                                             false, false, false, null);
        }

        throw new RuntimeException("Unable to parse point cut: " + string);
    }

    private ExtensionClassAndMethodMatcher getPointCut(Map attrs) {
        return YmlExtensionPointCutConverter
                       .createExtensionPointCut(attrs, defaultMetricPrefix, classLoader, extensionName);
    }

    public List<ExtensionClassAndMethodMatcher> getPointCuts(List list) throws ParseException {
        List pcs = new ArrayList();
        for (Iterator i$ = list.iterator(); i$.hasNext(); ) {
            Object obj = i$.next();
            pcs.add(getPointCut(obj));
        }
        return pcs;
    }

    public List<ExtensionClassAndMethodMatcher> getPointCuts(Map namesToPointCuts) throws ParseException {
        Collection values = null;

        if (null != namesToPointCuts) {
            values = namesToPointCuts.values();
        }
        if (null == values) {
            return Collections.EMPTY_LIST;
        }

        List pcs = new ArrayList();
        for (Iterator i$ = values.iterator(); i$.hasNext(); ) {
            Object obj = i$.next();
            if ((obj instanceof String)) {
                pcs.add(getPointCut((String) obj));
            } else if ((obj instanceof Map)) {
                pcs.add(getPointCut((Map) obj));
            }
        }
        return pcs;
    }

    public static class ClassMethodNameFormatDescriptor implements MetricNameFormatFactory {
        private final String prefix;

        public ClassMethodNameFormatDescriptor(String prefix, boolean dispatcher) {
            this.prefix = getMetricPrefix(prefix, dispatcher);
        }

        private static String getMetricPrefix(String prefix, boolean dispatcher) {
            if (dispatcher) {
                if (prefix.startsWith("OtherTransaction")) {
                    return prefix;
                }
                return "OtherTransaction/" + prefix;
            }

            return prefix;
        }

        public MetricNameFormat getMetricNameFormat(ClassMethodSignature sig, Object object, Object[] args) {
            if (Strings.isEmpty(prefix)) {
                return new ClassMethodMetricNameFormat(sig, object);
            }
            return new ClassMethodMetricNameFormat(sig, object, prefix);
        }
    }
}