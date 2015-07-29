package com.newrelic.agent.instrumentation.tracing;

import java.util.Arrays;
import java.util.List;

import com.newrelic.deps.com.google.common.collect.ImmutableList;
import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.util.Strings;

public class TraceDetailsBuilder {
    private final List<String> rollupMetricName = Lists.newArrayListWithCapacity(5);
    public List<ParameterAttributeName> parameterAttributeNames;
    private String metricPrefix;
    private String metricName;
    private String tracerFactoryName;
    private boolean dispatcher;
    private boolean excludeFromTransactionTrace;
    private boolean ignoreTransaction;
    private boolean nameTransaction;
    private boolean custom;
    private boolean webTransaction;
    private TransactionName transactionName;
    private List<InstrumentationType> instrumentationTypes = Lists.newArrayListWithCapacity(3);
    private List<String> instrumentationSourceNames = Lists.newArrayListWithCapacity(3);
    private boolean leaf;

    public static TraceDetailsBuilder newBuilder() {
        return new TraceDetailsBuilder();
    }

    public static TraceDetailsBuilder newBuilder(TraceDetails traceDetails) {
        TraceDetailsBuilder builder = new TraceDetailsBuilder();

        builder.custom = traceDetails.isCustom();
        builder.dispatcher = traceDetails.dispatcher();
        builder.excludeFromTransactionTrace = traceDetails.excludeFromTransactionTrace();
        builder.ignoreTransaction = traceDetails.ignoreTransaction();
        builder.instrumentationSourceNames = Lists.newArrayList(traceDetails.instrumentationSourceNames());
        builder.instrumentationTypes = Lists.newArrayList(traceDetails.instrumentationTypes());
        builder.metricName = traceDetails.metricName();
        builder.metricPrefix = traceDetails.metricPrefix();
        builder.transactionName = traceDetails.transactionName();
        builder.webTransaction = traceDetails.isWebTransaction();
        builder.leaf = traceDetails.isLeaf();
        builder.rollupMetricName.addAll(Arrays.asList(traceDetails.rollupMetricName()));
        builder.parameterAttributeNames = Lists.newArrayList(traceDetails.getParameterAttributeNames());

        return builder;
    }

    public static TraceDetails merge(TraceDetails existing, TraceDetails trace) {
        if (trace.isCustom()) {
            return newBuilder(trace).merge(existing).build();
        }
        return newBuilder(existing).merge(trace).build();
    }

    public TraceDetails build() {
        return new DefaultTraceDetails(this);
    }

    public TraceDetailsBuilder setParameterAttributeNames(List<ParameterAttributeName> reportedParams) {
        parameterAttributeNames = reportedParams;
        return this;
    }

    public TraceDetailsBuilder setMetricPrefix(String metricPrefix) {
        if (metricPrefix == null) {
            this.metricPrefix = null;
        } else {
            this.metricPrefix =
                    (metricPrefix.endsWith("/") ? metricPrefix.substring(0, metricPrefix.length() - 1) : metricPrefix);
        }

        return this;
    }

    public TraceDetailsBuilder setMetricName(String metricName) {
        this.metricName = metricName;
        return this;
    }

    public TraceDetailsBuilder setTracerFactoryName(String tracerFactoryName) {
        this.tracerFactoryName = tracerFactoryName;
        return this;
    }

    public TraceDetailsBuilder setDispatcher(boolean dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    public TraceDetailsBuilder setCustom(boolean custom) {
        this.custom = custom;
        return this;
    }

    public TraceDetailsBuilder setLeaf(boolean leaf) {
        this.leaf = leaf;
        return this;
    }

    public TraceDetailsBuilder setExcludeFromTransactionTrace(boolean excludeFromTransactionTrace) {
        this.excludeFromTransactionTrace = excludeFromTransactionTrace;
        return this;
    }

    public TraceDetailsBuilder setIgnoreTransaction(boolean ignoreTransaction) {
        this.ignoreTransaction = ignoreTransaction;
        return this;
    }

    public TraceDetailsBuilder setNameTransaction(boolean nameTransaction) {
        this.nameTransaction = nameTransaction;
        return this;
    }

    public TraceDetailsBuilder setTransactionName(TransactionNamePriority namingPriority, boolean override,
                                                  String category, String path) {
        transactionName = new TransactionName(namingPriority, override, category, path);
        return this;
    }

    public TraceDetailsBuilder setInstrumentationType(InstrumentationType type) {
        if (type == null) {
            instrumentationTypes = Lists.newArrayList(new InstrumentationType[] {InstrumentationType.Unknown});
        } else {
            instrumentationTypes = Lists.newArrayList(new InstrumentationType[] {type});
        }

        return this;
    }

    public TraceDetailsBuilder setInstrumentationSourceName(String instrumentationSourceName) {
        if (instrumentationSourceName == null) {
            instrumentationSourceNames = Lists.newArrayList(new String[] {"Unknown"});
        } else {
            instrumentationSourceNames = Lists.newArrayList(new String[] {instrumentationSourceName});
        }
        return this;
    }

    public TraceDetailsBuilder setWebTransaction(boolean webTransaction) {
        this.webTransaction = webTransaction;
        return this;
    }

    public TraceDetailsBuilder addRollupMetricName(String metricName) {
        rollupMetricName.add(metricName);
        return this;
    }

    public TraceDetailsBuilder merge(TraceDetails otherDetails) {
        if (metricPrefix == null) {
            metricPrefix = otherDetails.metricPrefix();
        }
        if (metricName == null) {
            metricName = otherDetails.metricName();
        }
        if (tracerFactoryName == null) {
            tracerFactoryName = otherDetails.tracerFactoryName();
        }
        if (!dispatcher) {
            dispatcher = otherDetails.dispatcher();
        }
        if (!excludeFromTransactionTrace) {
            excludeFromTransactionTrace = otherDetails.excludeFromTransactionTrace();
        }
        if ((!ignoreTransaction) && (!custom)) {
            ignoreTransaction = otherDetails.ignoreTransaction();
        }
        if (transactionName == null) {
            transactionName = otherDetails.transactionName();
        }
        if (!custom) {
            custom = otherDetails.isCustom();
            if (!leaf) {
                leaf = otherDetails.isLeaf();
            }
        }
        if (!webTransaction) {
            webTransaction = otherDetails.isWebTransaction();
        }
        rollupMetricName.addAll(Arrays.asList(otherDetails.rollupMetricName()));

        instrumentationTypes.addAll(otherDetails.instrumentationTypes());
        instrumentationSourceNames.addAll(otherDetails.instrumentationSourceNames());

        parameterAttributeNames.addAll(otherDetails.getParameterAttributeNames());

        return this;
    }

    private static final class DefaultTraceDetails implements TraceDetails {
        private final String metricPrefix;
        private final String metricName;
        private final String tracerFactoryName;
        private final TransactionName transactionName;
        private final boolean dispatcher;
        private final boolean excludeFromTransactionTrace;
        private final boolean ignoreTransaction;
        private final boolean custom;
        private final boolean webTransaction;
        private final List<InstrumentationType> instrumentationTypes;
        private final List<String> instrumentationSourceNames;
        private final boolean leaf;
        private final String[] rollupMetricNames;
        private final List<ParameterAttributeName> parameterAttributeNames;

        protected DefaultTraceDetails(TraceDetailsBuilder builder) {
            metricName = builder.metricName;
            metricPrefix = builder.metricPrefix;
            tracerFactoryName = builder.tracerFactoryName;
            dispatcher = builder.dispatcher;
            excludeFromTransactionTrace = builder.excludeFromTransactionTrace;
            ignoreTransaction = builder.ignoreTransaction;
            custom = builder.custom;
            if (builder.nameTransaction) {
                transactionName = (custom ? TransactionName.CUSTOM_DEFAULT : TransactionName.BUILT_IN_DEFAULT);
            } else {
                transactionName = builder.transactionName;
            }
            instrumentationSourceNames = Lists.newArrayList(builder.instrumentationSourceNames);
            instrumentationTypes = Lists.newArrayList(builder.instrumentationTypes);
            webTransaction = builder.webTransaction;
            leaf = builder.leaf;
            rollupMetricNames = ((String[]) builder.rollupMetricName.toArray(new String[0]));
            parameterAttributeNames =
                    (builder.parameterAttributeNames == null ? ImmutableList.<ParameterAttributeName>of()
                             : builder.parameterAttributeNames);
        }

        public boolean isLeaf() {
            return leaf;
        }

        public String metricName() {
            return metricName;
        }

        public boolean dispatcher() {
            return dispatcher;
        }

        public String tracerFactoryName() {
            return tracerFactoryName;
        }

        public boolean excludeFromTransactionTrace() {
            return excludeFromTransactionTrace;
        }

        public String metricPrefix() {
            return metricPrefix;
        }

        public String getFullMetricName(String className, String methodName) {
            if (metricName != null) {
                return metricName;
            }
            if (metricPrefix == null) {
                return null;
            }
            return Strings.join('/', new String[] {metricPrefix, "${className}", methodName});
        }

        public boolean ignoreTransaction() {
            return ignoreTransaction;
        }

        public boolean isCustom() {
            return custom;
        }

        public TransactionName transactionName() {
            return transactionName;
        }

        public List<InstrumentationType> instrumentationTypes() {
            return instrumentationTypes;
        }

        public List<String> instrumentationSourceNames() {
            return instrumentationSourceNames;
        }

        public boolean isWebTransaction() {
            return webTransaction;
        }

        public String toString() {
            return "DefaultTraceDetails [transactionName=" + transactionName + ", dispatcher=" + dispatcher
                           + ", custom=" + custom + ", instrumentationType=" + instrumentationTypes
                           + ", instrumentationSourceName=" + instrumentationSourceNames + "]";
        }

        public String[] rollupMetricName() {
            return rollupMetricNames;
        }

        public List<ParameterAttributeName> getParameterAttributeNames() {
            return parameterAttributeNames;
        }
    }
}