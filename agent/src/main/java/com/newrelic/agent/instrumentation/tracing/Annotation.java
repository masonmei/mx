package com.newrelic.agent.instrumentation.tracing;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.TransactionNamePriority;

public class Annotation extends AnnotationVisitor {
    private final TraceDetailsBuilder traceDetailsBuilder;
    private Map<String, Object> values;

    public Annotation(AnnotationVisitor annotationVisitor, String desc, TraceDetailsBuilder traceDetailsBuilder) {
        super(Agent.ASM_LEVEL, annotationVisitor);
        this.traceDetailsBuilder = traceDetailsBuilder;
    }

    public Map<String, Object> getValues() {
        return values == null ? Collections.<String, Object>emptyMap() : values;
    }

    public void visit(String name, Object value) {
        getOrCreateValues().put(name, value);
        super.visit(name, value);
    }

    private Map<String, Object> getOrCreateValues() {
        if (values == null) {
            values = Maps.newHashMap();
        }
        return values;
    }

    public AnnotationVisitor visitArray(String name) {
        List list = (List) getOrCreateValues().get(name);
        if (list == null) {
            list = Lists.newArrayList();
            getOrCreateValues().put(name, list);
        }

        final List theList = list;

        AnnotationVisitor av = super.visitArray(name);
        av = new AnnotationVisitor(Agent.ASM_LEVEL, av) {
            public void visit(String name, Object value) {
                super.visit(name, value);
                theList.add(value);
            }
        };
        return av;
    }

    private boolean getBoolean(String name) {
        Boolean value = (Boolean) getValues().get(name);
        return (value != null) && (value.booleanValue());
    }

    public TraceDetails getTraceDetails(boolean custom) {
        String metricName = (String) getValues().get("metricName");
        boolean dispatcher = getBoolean("dispatcher");

        if ((dispatcher) && (metricName != null)) {
            traceDetailsBuilder.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, false, "Custom", metricName);
        }

        List rollupMetricNames = (List) getValues().get("rollupMetricName");
        Iterator i$;
        if (rollupMetricNames != null) {
            for (i$ = rollupMetricNames.iterator(); i$.hasNext(); ) {
                Object v = i$.next();
                traceDetailsBuilder.addRollupMetricName(v.toString());
            }
        }

        return new DelegatingTraceDetails(traceDetailsBuilder.setMetricName(metricName).setDispatcher(dispatcher)
                                                  .setTracerFactoryName((String) getValues().get("tracerFactoryName"))
                                                  .setExcludeFromTransactionTrace(getBoolean("skipTransactionTrace"))
                                                  .setNameTransaction(getBoolean("nameTransaction")).setCustom(custom)
                                                  .setExcludeFromTransactionTrace(getBoolean
                                                                                          ("excludeFromTransactionTrace"))
                                                  .setLeaf(getBoolean("leaf")).build()) {
            public String getFullMetricName(String className, String methodName) {
                return metricName();
            }
        };
    }
}