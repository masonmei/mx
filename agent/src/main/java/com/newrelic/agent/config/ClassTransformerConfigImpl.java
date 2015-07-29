package com.newrelic.agent.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.annotationmatchers.AnnotationMatcher;
import com.newrelic.agent.instrumentation.annotationmatchers.ClassNameAnnotationMatcher;
import com.newrelic.agent.instrumentation.annotationmatchers.NoMatchAnnotationMatcher;
import com.newrelic.agent.instrumentation.annotationmatchers.OrAnnotationMatcher;
import com.newrelic.deps.org.objectweb.asm.Type;

final class ClassTransformerConfigImpl extends BaseConfig implements ClassTransformerConfig {
    public static final String ENABLED = "enabled";
    public static final String EXCLUDES = "excludes";
    public static final String INCLUDES = "includes";
    public static final String COMPUTE_FRAMES = "compute_frames";
    public static final String SHUTDOWN_DELAY = "shutdown_delay";
    public static final String GRANT_PACKAGE_ACCESS = "grant_package_access";
    public static final boolean DEFAULT_COMPUTE_FRAMES = true;
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_SHUTDOWN_DELAY = -1;
    public static final boolean DEFAULT_GRANT_PACKAGE_ACCESS = false;
    public static final String JDBC_STATEMENTS_PROPERTY = "jdbc_statements";
    static final String NEW_RELIC_TRACE_TYPE_DESC = "Lcom/newrelic/api/agent/Trace;";
    static final String DEPRECATED_NEW_RELIC_TRACE_TYPE_DESC = "Lcom/newrelic/agent/Trace;";
    private static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.class_transformer.";
    private final boolean isEnabled;
    private final boolean custom_tracing;
    private final Set<String> excludes;
    private final Set<String> includes;
    private final boolean computeFrames;
    private final long shutdownDelayInNanos;
    private final boolean grantPackageAccess;
    private final AnnotationMatcher ignoreTransactionAnnotationMatcher;
    private final AnnotationMatcher ignoreApdexAnnotationMatcher;
    private final AnnotationMatcher traceAnnotationMatcher;

    ClassTransformerConfigImpl(Map<String, Object> props, boolean customTracingEnabled) {
        super(props, "newrelic.config.class_transformer.");
        custom_tracing = customTracingEnabled;
        isEnabled = ((Boolean) getProperty("enabled", Boolean.valueOf(true))).booleanValue();
        excludes = Collections.unmodifiableSet(new HashSet(getUniqueStrings("excludes")));
        includes = Collections.unmodifiableSet(new HashSet(getUniqueStrings("includes")));
        computeFrames = ((Boolean) getProperty("compute_frames", Boolean.valueOf(true))).booleanValue();
        shutdownDelayInNanos = initShutdownDelay();
        grantPackageAccess = ((Boolean) getProperty("grant_package_access", Boolean.valueOf(false))).booleanValue();

        traceAnnotationMatcher =
                (customTracingEnabled ? initializeTraceAnnotationMatcher(props) : new NoMatchAnnotationMatcher());

        ignoreTransactionAnnotationMatcher = new ClassNameAnnotationMatcher("NewRelicIgnoreTransaction", false);

        ignoreApdexAnnotationMatcher = new ClassNameAnnotationMatcher("NewRelicIgnoreApdex", false);
    }

    static String internalizeName(String name) {
        return 'L' + name.trim().replace('.', '/') + ';';
    }

    static ClassTransformerConfig createClassTransformerConfig(Map<String, Object> settings, boolean custom_tracing) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new ClassTransformerConfigImpl(settings, custom_tracing);
    }

    private AnnotationMatcher initializeTraceAnnotationMatcher(Map<?, ?> props) {
        List matchers = new ArrayList();
        matchers.add(new ClassNameAnnotationMatcher(Type.getType("Lcom/newrelic/agent/Trace;").getDescriptor()));
        matchers.add(new ClassNameAnnotationMatcher(Type.getType("Lcom/newrelic/api/agent/Trace;").getDescriptor()));
        String traceAnnotationClassName = (String) getProperty("trace_annotation_class_name");
        if (traceAnnotationClassName == null) {
            matchers.add(new ClassNameAnnotationMatcher("NewRelicTrace", false));
        } else {
            final HashSet names = new HashSet();
            for (String name : traceAnnotationClassName.split(",")) {
                Agent.LOG.fine("Adding " + name + " as a Trace annotation");
                names.add(internalizeName(name));
            }
            matchers.add(new AnnotationMatcher() {
                public boolean matches(String annotationDesc) {
                    return names.contains(annotationDesc);
                }
            });
        }

        return OrAnnotationMatcher.getOrMatcher((AnnotationMatcher[]) matchers.toArray(new AnnotationMatcher[0]));
    }

    private long initShutdownDelay() {
        int shutdownDelayInSeconds = getIntProperty("shutdown_delay", -1);
        if (shutdownDelayInSeconds > 0) {
            return TimeUnit.NANOSECONDS.convert(shutdownDelayInSeconds, TimeUnit.SECONDS);
        }
        return -1L;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isCustomTracingEnabled() {
        return custom_tracing;
    }

    public Set<String> getIncludes() {
        return includes;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

    public boolean computeFrames() {
        return computeFrames;
    }

    public boolean isGrantPackageAccess() {
        return grantPackageAccess;
    }

    public long getShutdownDelayInNanos() {
        return shutdownDelayInNanos;
    }

    public final AnnotationMatcher getIgnoreTransactionAnnotationMatcher() {
        return ignoreTransactionAnnotationMatcher;
    }

    public final AnnotationMatcher getIgnoreApdexAnnotationMatcher() {
        return ignoreApdexAnnotationMatcher;
    }

    public AnnotationMatcher getTraceAnnotationMatcher() {
        return traceAnnotationMatcher;
    }

    public Collection<String> getJdbcStatements() {
        String jdbcStatementsProp = (String) getProperty("jdbc_statements");
        if (jdbcStatementsProp == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(jdbcStatementsProp.split(",[\\s]*"));
    }

    public Config getInstrumentationConfig(String implementationTitle) {
        Map config = Collections.emptyMap();
        if (implementationTitle != null) {
            Object pointCutConfig = getProperty(implementationTitle);
            if ((pointCutConfig instanceof Map)) {
                config = (Map) pointCutConfig;
            }
        }
        return new BaseConfig(config);
    }
}