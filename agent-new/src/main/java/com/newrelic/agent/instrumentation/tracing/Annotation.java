package com.newrelic.agent.instrumentation.tracing;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;

public class Annotation extends AnnotationVisitor {
    private final TraceDetailsBuilder traceDetailsBuilder;
    private Map<String, Object> values;

    public Annotation(AnnotationVisitor annotationVisitor, String desc, TraceDetailsBuilder traceDetailsBuilder) {
        super(327680, annotationVisitor);
        this.traceDetailsBuilder = traceDetailsBuilder;
    }

    public Map<String, Object> getValues() {
        return this.values == null ? Collections.<String, Object>emptyMap() : this.values;
    }

    public void visit(String name, Object value) {
        getOrCreateValues().put(name, value);
        super.visit(name, value);
    }

    private Map<String, Object> getOrCreateValues() {
        if (this.values == null) {
            this.values = Maps.newHashMap();
        }
        return this.values;
    }

    public AnnotationVisitor visitArray(String name) {
        List list = (List) getOrCreateValues().get(name);
        if (list == null) {
            list = Lists.newArrayList();
            getOrCreateValues().put(name, list);
        }

        final List theList = list;

        AnnotationVisitor av = super.visitArray(name);
        av = new AnnotationVisitor(327680, av) {
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
            this.traceDetailsBuilder
                    .setTransactionName(TransactionNamePriority.CUSTOM_HIGH, false, "Custom", metricName);
        }

        List rollupMetricNames = (List) getValues().get("rollupMetricName");
        Iterator i$;
        if (rollupMetricNames != null) {
            for (i$ = rollupMetricNames.iterator(); i$.hasNext(); ) {
                Object v = i$.next();
                this.traceDetailsBuilder.addRollupMetricName(v.toString());
            }
        }

        return new DelegatingTraceDetails(this.traceDetailsBuilder.setMetricName(metricName).setDispatcher(dispatcher)
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