//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.trace;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Connection;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.DatabaseVendor;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.instrumentation.pointcuts.database.ConnectionFactory;
import com.newrelic.agent.instrumentation.pointcuts.database.DatabaseUtils;
import com.newrelic.agent.instrumentation.pointcuts.database.ExplainPlanExecutor;
import com.newrelic.agent.instrumentation.pointcuts.database.SqlStatementTracer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transport.DataSenderWriter;

public class TransactionTrace implements Comparable<TransactionTrace>, JSONStreamAware {
    private final TransactionSegment rootSegment;
    private final List<TransactionSegment> sqlSegments;
    private final Map<ConnectionFactory, List<ExplainPlanExecutor>> sqlTracers;
    private final long duration;
    private final long startTime;
    private final String rootMetricName;
    private final Map<String, Object> userAttributes;
    private final Map<String, Object> agentAttributes;
    private final Map<String, Object> intrinsicAttributes;
    private final long rootTracerStartTime;
    private final String guid;
    private final Map<String, Map<String, String>> prefixedAttributes;
    private final String applicationName;
    private String requestUri;
    private Map<Tracer, Collection<Tracer>> children;
    private Long xraySessionId;
    private String syntheticsResourceId;

    private TransactionTrace(TransactionData transactionData, SqlObfuscator sqlObfuscator) {
        this.applicationName = transactionData.getApplicationName();
        this.children = buildChildren(transactionData.getTracers());
        this.sqlTracers = new HashMap();
        Tracer tracer = transactionData.getRootTracer();
        this.userAttributes = Maps.newHashMap();
        this.agentAttributes = Maps.newHashMap();
        if (ServiceFactory.getAttributesService().isAttributesEnabledForTraces(this.applicationName)) {
            if (transactionData.getAgentAttributes() != null) {
                this.agentAttributes.putAll(transactionData.getAgentAttributes());
            }

            if (transactionData.getUserAttributes() != null) {
                this.userAttributes.putAll(transactionData.getUserAttributes());
            }
        }

        this.prefixedAttributes = transactionData.getPrefixedAttributes();
        this.intrinsicAttributes = Maps.newHashMap();
        if (transactionData.getIntrinsicAttributes() != null) {
            this.intrinsicAttributes.putAll(transactionData.getIntrinsicAttributes());
        }

        this.startTime = transactionData.getWallClockStartTimeMs();
        this.rootTracerStartTime = tracer.getStartTimeInMilliseconds();
        this.sqlSegments = new LinkedList();
        this.requestUri = transactionData.getRequestUri();
        if (this.requestUri == null || this.requestUri.length() == 0) {
            this.requestUri = "/ROOT";
        }

        this.rootMetricName = transactionData.getBlameOrRootMetricName();
        this.guid = transactionData.getGuid();
        this.rootSegment = new TransactionSegment(transactionData.getTransactionTracerConfig(), sqlObfuscator,
                                                         this.rootTracerStartTime, tracer,
                                                         this.createTransactionSegment(transactionData
                                                                                               .getTransactionTracerConfig(),
                                                                                              sqlObfuscator, tracer,
                                                                                              (TransactionSegment)
                                                                                                      null));
        this.rootSegment.setMetricName("ROOT");
        this.duration = tracer.getDurationInMilliseconds();
        Long gcTime = (Long) this.intrinsicAttributes.remove("gc_time");
        if (gcTime != null) {
            float cpuTime = (float) gcTime.longValue() / 1.0E9F;
            this.intrinsicAttributes.put("gc_time", Float.valueOf(cpuTime));
        }

        Long cpuTime1 = (Long) this.intrinsicAttributes.remove("cpu_time");
        if (cpuTime1 != null) {
            float cpuTimeInSecs = (float) cpuTime1.longValue() / 1.0E9F;
            this.intrinsicAttributes.put("cpu_time", Float.valueOf(cpuTimeInSecs));
        }

        this.children.clear();
        this.children = null;
        this.xraySessionId = null;
        this.syntheticsResourceId = null;
    }

    private static Map<Tracer, Collection<Tracer>> buildChildren(Collection<Tracer> tracers) {
        if (tracers != null && !tracers.isEmpty()) {
            HashMap children = new HashMap();

            Tracer tracer;
            Object kids;
            for (Iterator i$ = tracers.iterator(); i$.hasNext(); ((Collection) kids).add(tracer)) {
                tracer = (Tracer) i$.next();
                Tracer parentTracer = tracer.getParentTracer();
                kids = (Collection) children.get(parentTracer);
                if (kids == null) {
                    kids = new LinkedList();
                    children.put(parentTracer, kids);
                }
            }

            return children;
        } else {
            return Collections.emptyMap();
        }
    }

    private static SqlObfuscator getSqlObfuscator(String appName) {
        SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getSqlObfuscator(appName);
        return SqlObfuscator.getCachingSqlObfuscator(sqlObfuscator);
    }

    public static TransactionTrace getTransactionTrace(TransactionData td) {
        return getTransactionTrace(td, getSqlObfuscator(td.getApplicationName()));
    }

    static TransactionTrace getTransactionTrace(TransactionData transactionData, SqlObfuscator sqlObfuscator) {
        return new TransactionTrace(transactionData, sqlObfuscator);
    }

    public TransactionSegment getRootSegment() {
        return this.rootSegment;
    }

    private TransactionSegment createTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
                                                        Tracer tracer, TransactionSegment lastSibling) {
        TransactionSegment segment =
                tracer.getTransactionSegment(ttConfig, sqlObfuscator, this.rootTracerStartTime, lastSibling);
        this.processSqlTracer(tracer);
        Collection children = (Collection) this.children.get(tracer);
        if (children != null) {
            TransactionSegment lastKid = null;
            Iterator i$ = children.iterator();

            while (i$.hasNext()) {
                Tracer child = (Tracer) i$.next();
                if (child.getTransactionSegmentName() != null) {
                    TransactionSegment childSegment =
                            this.createTransactionSegment(ttConfig, sqlObfuscator, child, lastKid);
                    if (childSegment != lastKid) {
                        this.addChildSegment(segment, childSegment);
                        lastKid = childSegment;
                    }
                }
            }
        }

        return segment;
    }

    public Map<ConnectionFactory, List<ExplainPlanExecutor>> getExplainPlanExecutors() {
        return Collections.unmodifiableMap(this.sqlTracers);
    }

    private void processSqlTracer(Tracer tracer) {
        if (tracer instanceof SqlStatementTracer) {
            SqlStatementTracer sqlTracer = (SqlStatementTracer) tracer;
            ExplainPlanExecutor explainExecutor = sqlTracer.getExplainPlanExecutor();
            ConnectionFactory connectionFactory = sqlTracer.getConnectionFactory();
            if (!sqlTracer.hasExplainPlan() && explainExecutor != null && connectionFactory != null) {
                List<ExplainPlanExecutor> tracers = this.sqlTracers.get(connectionFactory);
                if (tracers == null) {
                    tracers = new LinkedList();
                    this.sqlTracers.put(connectionFactory, tracers);
                }

                ((List) tracers).add(explainExecutor);
            }
        }

    }

    private void addChildSegment(TransactionSegment parent, TransactionSegment child) {
        if (child.getMetricName() == null) {
            Iterator i$ = child.getChildren().iterator();

            while (i$.hasNext()) {
                TransactionSegment kid = (TransactionSegment) i$.next();
                this.addChildSegment(parent, kid);
            }
        } else {
            parent.addChild(child);
        }

    }

    private void runExplainPlans() {
        if (!this.sqlTracers.isEmpty()) {
            DatabaseService dbService = ServiceFactory.getDatabaseService();
            Iterator i$ = this.sqlTracers.entrySet().iterator();

            while (i$.hasNext()) {
                Entry entry = (Entry) i$.next();
                Agent.LOG.finer(MessageFormat.format("Running {0} explain plan(s)",
                                                            new Object[] {Integer.valueOf(((List) entry.getValue())
                                                                                                  .size())}));
                Connection connection = null;

                try {
                    connection = ((ConnectionFactory) entry.getKey()).getConnection();
                    DatabaseVendor e = DatabaseUtils.getDatabaseVendor(connection);
                    Iterator msg1 = ((List) entry.getValue()).iterator();

                    while (msg1.hasNext()) {
                        ExplainPlanExecutor explainExecutor = (ExplainPlanExecutor) msg1.next();
                        if (explainExecutor != null) {
                            explainExecutor.runExplainPlan(dbService, connection, e);
                        }
                    }
                } catch (Throwable var16) {
                    String msg = MessageFormat.format("An error occurred executing an explain plan: {0}",
                                                             new Object[] {var16.toString()});
                    if (Agent.LOG.isLoggable(Level.FINER)) {
                        Agent.LOG.log(Level.FINER, msg, var16);
                    } else {
                        Agent.LOG.fine(msg);
                    }
                } finally {
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (Exception var15) {
                            Agent.LOG.log(Level.FINER, "Unable to close connection", var15);
                        }
                    }

                }
            }

            this.sqlTracers.clear();
        }

    }

    private Map<String, Object> getAgentAtts() {
        HashMap atts = Maps.newHashMap();
        atts.putAll(this.agentAttributes);
        if (this.prefixedAttributes != null && !this.prefixedAttributes.isEmpty()) {
            atts.putAll(AttributesUtils.appendAttributePrefixes(this.prefixedAttributes));
        }

        return atts;
    }

    private void filterAndAddIfNotEmpty(String key, Map<String, Object> wheretoAdd, Map<String, Object> toAdd) {
        Map output = ServiceFactory.getAttributesService().filterTraceAttributes(this.applicationName, toAdd);
        if (output != null && !output.isEmpty()) {
            wheretoAdd.put(key, output);
        }

    }

    private Map<String, Object> getAttributes() {
        HashMap attributes = new HashMap();
        if (ServiceFactory.getAttributesService().isAttributesEnabledForTraces(this.applicationName)) {
            this.filterAndAddIfNotEmpty("agentAttributes", attributes, this.getAgentAtts());
            if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                this.filterAndAddIfNotEmpty("userAttributes", attributes, this.userAttributes);
            }
        }

        if (this.intrinsicAttributes != null && !this.intrinsicAttributes.isEmpty()) {
            attributes.put("intrinsics", this.intrinsicAttributes);
        }

        return attributes;
    }

    public void writeJSONString(Writer writer) throws IOException {
        this.runExplainPlans();
        boolean forcePersist = false;
        List data =
                Arrays.asList(new Object[] {Long.valueOf(this.startTime), Collections.EMPTY_MAP, Collections.EMPTY_MAP,
                                                   this.rootSegment, this.getAttributes()});
        if (null == this.xraySessionId && null == this.syntheticsResourceId) {
            JSONArray.writeJSONString(Arrays.asList(new Serializable[] {Long.valueOf(this.startTime),
                                                                               Long.valueOf(this.duration),
                                                                               this.rootMetricName, this.requestUri,
                                                                               DataSenderWriter
                                                                                       .getJsonifiedCompressedEncodedString(data,
                                                                                                                                   writer),
                                                                               this.guid, null,
                                                                               Boolean.valueOf(false)}), writer);
        } else if (null == this.syntheticsResourceId) {
            JSONArray.writeJSONString(Arrays.asList(new Serializable[] {Long.valueOf(this.startTime),
                                                                               Long.valueOf(this.duration),
                                                                               this.rootMetricName, this.requestUri,
                                                                               DataSenderWriter
                                                                                       .getJsonifiedCompressedEncodedString(data,
                                                                                                                                   writer),
                                                                               this.guid, null, Boolean.valueOf(true),
                                                                               this.xraySessionId}), writer);
        } else {
            JSONArray.writeJSONString(Arrays.asList(new Serializable[] {Long.valueOf(this.startTime),
                                                                               Long.valueOf(this.duration),
                                                                               this.rootMetricName, this.requestUri,
                                                                               DataSenderWriter
                                                                                       .getJsonifiedCompressedEncodedString(data,
                                                                                                                                   writer),
                                                                               this.guid, null, Boolean.valueOf(true),
                                                                               this.xraySessionId,
                                                                               this.syntheticsResourceId}), writer);
        }

    }

    protected List<TransactionSegment> getSQLSegments() {
        return this.sqlSegments;
    }

    public String toString() {
        return MessageFormat.format("{0} {1} ms", new Object[] {this.requestUri, Long.valueOf(this.duration)});
    }

    public int compareTo(TransactionTrace o) {
        return (int) (this.duration - o.duration);
    }

    public long getDuration() {
        return this.duration;
    }

    public String getRequestUri() {
        return this.requestUri;
    }

    public Long getXraySessionId() {
        return this.xraySessionId;
    }

    public void setXraySessionId(Long xraySessionId) {
        this.xraySessionId = xraySessionId;
    }

    public String getSyntheticsResourceId() {
        return this.syntheticsResourceId;
    }

    public void setSyntheticsResourceId(String syntheticsResourceId) {
        this.syntheticsResourceId = syntheticsResourceId;
    }

    public String getRootMetricName() {
        return this.rootMetricName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }
}
