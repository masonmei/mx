package com.newrelic.agent;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.cache.Cache;
import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.agent.browser.BrowserTransactionState;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.DatabaseStatementParser;
import com.newrelic.agent.database.ParsedDatabaseStatement;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.instrumentation.pointcuts.database.ConnectionFactory;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.sql.NopSqlTracerListener;
import com.newrelic.agent.sql.SqlTracerListener;
import com.newrelic.agent.stats.AbstractMetricAggregator;
import com.newrelic.agent.stats.ApdexStats;
import com.newrelic.agent.stats.ApdexStatsImpl;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.ResponseTimeStatsImpl;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.Stats;
import com.newrelic.agent.stats.StatsBase;
import com.newrelic.agent.stats.StatsImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.MetricNameFormatWithHost;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import com.newrelic.agent.transaction.ConnectionCache;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionCache;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class DummyTransaction extends Transaction {
    private static final MetricAggregator metricAggregator = new AbstractMetricAggregator() {
        protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime, TimeUnit timeUnit) {
        }

        protected void doRecordMetric(String name, float value) {
        }

        protected void doIncrementCounter(String name, int count) {
        }
    };
    private final IAgent agent;
    private final String guid;
    private final Map<String, String> dummyMap = new DummyMap();
    private final Map<String, Object> dummyObjectMap = new DummyMap();
    private final Map<String, Map<String, String>> dummyStringMap = new DummyMap();
    private final Object lock = new Object();
    private final Insights insights = new DummyInsights();
    private final AtomicReference<AppNameAndConfig> appNameAndConfig =
            new AtomicReference(new AppNameAndConfig(PriorityApplicationName.NONE, null));
    private final TracerList tracerList = new TracerList(null, new DummySet());
    private final TransactionTimer timer = new TransactionTimer(0L);
    private final InboundHeaderState inboundHeaderState = new InboundHeaderState(null, null);
    private final SqlTracerListener sqlTracerListener = new NopSqlTracerListener();
    private final boolean autoAppNamingEnabled;
    private final TransactionCounts txnCounts;
    private final Set<TransactionActivity> finishedChildren = new DummySet();

    protected DummyTransaction() {
        agent = ServiceFactory.getAgent();
        guid = TransactionGuidFactory.generateGuid();
        AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        autoAppNamingEnabled = defaultConfig.isAutoAppNamingEnabled();
        txnCounts = new TransactionCounts(defaultConfig);
    }

    public MetricAggregator getMetricAggregator() {
        return metricAggregator;
    }

    public IAgent getAgent() {
        return agent;
    }

    public Object getLock() {
        return lock;
    }

    public String getGuid() {
        return guid;
    }

    public AgentConfig getAgentConfig() {
        AgentConfig config = null;
        do {
            AppNameAndConfig nc = (AppNameAndConfig) appNameAndConfig.get();
            config = nc.config;
            if (config == null) {
                config = ServiceFactory.getConfigService().getAgentConfig(nc.name.getName());
                if (!appNameAndConfig.compareAndSet(nc, new AppNameAndConfig(nc.name, config))) {
                    config = null;
                }
            }
        } while (config == null);
        return config;
    }

    public long getWallClockStartTimeMs() {
        return 0L;
    }

    public Map<String, Object> getInternalParameters() {
        return dummyObjectMap;
    }

    public Map<String, Map<String, String>> getPrefixedAgentAttributes() {
        return dummyStringMap;
    }

    public Map<String, Object> getUserAttributes() {
        return dummyObjectMap;
    }

    public Map<String, Object> getAgentAttributes() {
        return dummyObjectMap;
    }

    public Map<String, Object> getIntrinsicAttributes() {
        return dummyObjectMap;
    }

    public Map<String, String> getErrorAttributes() {
        return dummyMap;
    }

    public Insights getInsightsData() {
        return insights;
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return getAgentConfig().getTransactionTracerConfig();
    }

    public CrossProcessConfig getCrossProcessConfig() {
        return getAgentConfig().getCrossProcessConfig();
    }

    public boolean setTransactionName(com.newrelic.api.agent.TransactionNamePriority namePriority, boolean override,
                                      String category, String[] parts) {
        return false;
    }

    public boolean setTransactionName(com.newrelic.agent.bridge.TransactionNamePriority namePriority, boolean override,
                                      String category, String[] parts) {
        return false;
    }

    public boolean isTransactionNameSet() {
        return false;
    }

    public PriorityTransactionName getPriorityTransactionName() {
        return PriorityTransactionName.NONE;
    }

    public void freezeTransactionName() {
    }

    public boolean conditionalSetPriorityTransactionName(TransactionNamingPolicy policy, String name, String category,
                                                         com.newrelic.agent.bridge.TransactionNamePriority priority) {
        return false;
    }

    public boolean setPriorityTransactionName(PriorityTransactionName ptn) {
        return false;
    }

    public SqlTracerListener getSqlTracerListener() {
        return sqlTracerListener;
    }

    public TransactionCache getTransactionCache() {
        return DummyTransactionCache.INSTANCE;
    }

    public ConnectionCache getConnectionCache() {
        return DummyConnectionCache.INSTANCE;
    }

    public boolean isStarted() {
        return false;
    }

    public boolean isFinished() {
        return true;
    }

    public boolean isInProgress() {
        return false;
    }

    public Dispatcher getDispatcher() {
        return null;
    }

    public void setDispatcher(Dispatcher dispatcher) {
    }

    public long getExternalTime() {
        return 0L;
    }

    public Tracer getRootTracer() {
        return null;
    }

    public List<Tracer> getAllTracers() {
        return getTracers();
    }

    public List<Tracer> getTracers() {
        return tracerList;
    }

    public TransactionActivity getTransactionActivity() {
        return DummyTransactionActivity.INSTANCE;
    }

    public TransactionActivity getInitialTransactionActivity() {
        return null;
    }

    void activityStarted(TransactionActivity activity) {
    }

    public void startTransactionIfBeginning(Tracer tracer) {
    }

    public TransactionTimer getTransactionTimer() {
        return timer;
    }

    public void addTotalCpuTimeForLegacy(long time) {
    }

    public void recordFinalGCTime(TransactionStats stats) {
    }

    public boolean isTransactionTraceEnabled() {
        return false;
    }

    public boolean isAutoAppNamingEnabled() {
        return autoAppNamingEnabled;
    }

    public boolean isTransactionNamingEnabled() {
        return false;
    }

    public boolean isWebTransaction() {
        return false;
    }

    public boolean isAsyncTransaction() {
        return false;
    }

    public boolean isSynthetic() {
        return false;
    }

    public void provideHeaders(InboundHeaders headers) {
    }

    public void provideRawHeaders(InboundHeaders headers) {
    }

    public InboundHeaderState getInboundHeaderState() {
        return inboundHeaderState;
    }

    public IRPMService getRPMService() {
        return ServiceFactory.getRPMServiceManager().getOrCreateRPMService(getPriorityApplicationName());
    }

    public String getNormalizedUri() {
        return null;
    }

    public void setNormalizedUri(String normalizedUri) {
    }

    public Throwable getReportError() {
        return null;
    }

    public int getStatus() {
        return 0;
    }

    public String getStatusMessage() {
        return null;
    }

    public void freezeStatus() {
    }

    public void setThrowable(Throwable throwable, TransactionErrorPriority priority) {
    }

    public boolean isIgnore() {
        return true;
    }

    public void setIgnore(boolean ignore) {
    }

    public void ignore() {
    }

    public void ignoreApdex() {
    }

    public TransactionCounts getTransactionCounts() {
        return txnCounts;
    }

    public boolean shouldGenerateTransactionSegment() {
        return false;
    }

    public DatabaseStatementParser getDatabaseStatementParser() {
        return DummyDatabaseStatementParser.INSTANCE;
    }

    public BrowserTransactionState getBrowserTransactionState() {
        return null;
    }

    public CrossProcessState getCrossProcessState() {
        return DummyCrossProcessState.INSTANCE;
    }

    public CrossProcessTransactionState getCrossProcessTransactionState() {
        return DummyCrossProcessState.INSTANCE;
    }

    public TransactionState getTransactionState() {
        return DummyTransactionState.INSTANCE;
    }

    public void setTransactionState(TransactionState transactionState) {
    }

    public Transaction getRootTransaction() {
        return this;
    }

    public void setRootTransaction(Transaction tx) {
    }

    public void beforeSendResponseHeaders() {
    }

    public WebResponse getWebResponse() {
        return DEFAULT_RESPONSE;
    }

    public void setWebResponse(Response response) {
    }

    public void convertToWebTransaction() {
    }

    public void requestInitialized(Request request, Response response) {
    }

    public void requestDestroyed() {
    }

    public boolean isWebRequestSet() {
        return false;
    }

    public boolean isWebResponseSet() {
        return false;
    }

    public void setWebRequest(Request request) {
    }

    public String getApplicationName() {
        return getPriorityApplicationName().getName();
    }

    public PriorityApplicationName getPriorityApplicationName() {
        return PriorityApplicationName.NONE;
    }

    public void setApplicationName(ApplicationNamePriority priority, String appName) {
    }

    public long getRunningDurationInNanos() {
        return 0L;
    }

    public void saveMessageParameters(Map<String, String> parameters) {
    }

    public boolean registerAsyncActivity(Object activityContext) {
        return false;
    }

    public boolean startAsyncActivity(Object activityContext) {
        return false;
    }

    public void timeoutAsyncActivity(Object activityContext) {
    }

    public boolean ignoreAsyncActivity(Object activityContext) {
        return false;
    }

    public Set<TransactionActivity> getFinishedChildren() {
        return finishedChildren;
    }

    public void activityFinished(TransactionActivity activity, Tracer tracer, int opcode) {
    }

    public void activityFailed(TransactionActivity activity, int opcode) {
    }

    static final class DummyTransactionState implements TransactionState {
        public static final TransactionState INSTANCE = new DummyTransactionState();

        public Tracer getTracer(Transaction tx, TracerFactory tracerFactory, ClassMethodSignature sig, Object obj,
                                Object[] args) {
            return null;
        }

        public Tracer getTracer(Transaction tx, String tracerFactoryName, ClassMethodSignature sig, Object obj,
                                Object[] args) {
            return null;
        }

        public Tracer getTracer(Transaction tx, Object invocationTarget, ClassMethodSignature sig, String metricName,
                                int flags) {
            return null;
        }

        public boolean finish(Transaction tx, Tracer tracer) {
            return false;
        }

        public void resume() {
        }

        public void suspend() {
        }

        public void suspendRootTracer() {
        }

        public void complete() {
        }

        public void asyncJobStarted(TransactionHolder job) {
        }

        public void asyncJobFinished(TransactionHolder job) {
        }

        public void asyncTransactionStarted(Transaction tx, TransactionHolder txHolder) {
        }

        public void asyncTransactionFinished(TransactionActivity txa) {
        }

        public void mergeAsyncTracers() {
        }

        public Tracer getRootTracer() {
            return null;
        }

        public void asyncJobInvalidate(TransactionHolder job) {
        }

        public void setInvalidateAsyncJobs(boolean invalidate) {
        }
    }

    static final class DummyTransactionCache extends TransactionCache {
        public static final TransactionCache INSTANCE = new DummyTransactionCache();

        public MetricNameFormatWithHost getMetricNameFormatWithHost(Object key) {
            return null;
        }

        public void putMetricNameFormatWithHost(Object key, MetricNameFormatWithHost val) {
        }

        public Object removeSolrResponseBuilderParamName() {
            return null;
        }

        public void putSolrResponseBuilderParamName(Object val) {
        }

        public URL getURL(Object key) {
            return null;
        }

        public void putURL(Object key, URL val) {
        }
    }

    static final class DummyConnectionCache extends ConnectionCache {
        static final ConnectionCache INSTANCE = new DummyConnectionCache();

        public void putConnectionFactory(Connection key, ConnectionFactory val) {
        }

        public long getConnectionFactoryCacheSize() {
            return 0L;
        }

        public ConnectionFactory removeConnectionFactory(Connection key) {
            return null;
        }

        public Cache<Connection, ConnectionFactory> getConnectionFactoryCache() {
            return null;
        }

        public void clear() {
        }
    }

    static final class DummyApdexStat extends ApdexStatsImpl {
        public Object clone() throws CloneNotSupportedException {
            return this;
        }

        public String toString() {
            return "";
        }

        public void recordApdexFrustrated() {
        }

        public int getApdexSatisfying() {
            return 0;
        }

        public int getApdexTolerating() {
            return 0;
        }

        public int getApdexFrustrating() {
            return 0;
        }

        public void recordApdexResponseTime(long responseTimeMillis, long apdexTInMillis) {
        }

        public boolean hasData() {
            return false;
        }

        public void reset() {
        }

        public void writeJSONString(Writer writer) throws IOException {
        }

        public void merge(StatsBase statsObj) {
        }
    }

    static final class DummyResponseTimeStats extends ResponseTimeStatsImpl {
        public Object clone() throws CloneNotSupportedException {
            return this;
        }

        public void recordResponseTime(long responseTime, TimeUnit timeUnit) {
        }

        public void recordResponseTime(long responseTime, long exclusiveTime, TimeUnit timeUnit) {
        }

        public void recordResponseTimeInNanos(long responseTime) {
        }

        public void recordResponseTimeInNanos(long responseTime, long exclusiveTime) {
        }

        public boolean hasData() {
            return false;
        }

        public void reset() {
        }

        public float getTotal() {
            return 0.0F;
        }

        public float getTotalExclusiveTime() {
            return 0.0F;
        }

        public float getMaxCallTime() {
            return 0.0F;
        }

        public float getMinCallTime() {
            return 0.0F;
        }

        public double getSumOfSquares() {
            return 0.0D;
        }

        public void recordResponseTime(int count, long totalTime, long minTime, long maxTime, TimeUnit unit) {
        }

        public String toString() {
            return "";
        }

        public void incrementCallCount(int value) {
        }

        public void incrementCallCount() {
        }

        public int getCallCount() {
            return 0;
        }

        public void setCallCount(int count) {
        }
    }

    static final class DummyStats extends StatsImpl {
        public DummyStats() {
        }

        public DummyStats(int count, float total, float minValue, float maxValue, double sumOfSquares) {
        }

        public Object clone() throws CloneNotSupportedException {
            return this;
        }

        public String toString() {
            return "";
        }

        public void recordDataPoint(float value) {
        }

        public boolean hasData() {
            return false;
        }

        public void reset() {
        }

        public float getTotal() {
            return 0.0F;
        }

        public float getTotalExclusiveTime() {
            return 0.0F;
        }

        public float getMinCallTime() {
            return 0.0F;
        }

        public float getMaxCallTime() {
            return 0.0F;
        }

        public double getSumOfSquares() {
            return 0.0D;
        }

        public void merge(StatsBase statsObj) {
        }

        public void incrementCallCount(int value) {
        }

        public void incrementCallCount() {
        }

        public int getCallCount() {
            return 0;
        }

        public void setCallCount(int count) {
        }
    }

    static final class DummySimpleStatsEngine extends SimpleStatsEngine {
        static final Map<String, StatsBase> statsMap = new DummyMap();
        static final DummyStats stat = new DummyStats();
        static final DummyResponseTimeStats responseTimeStat = new DummyResponseTimeStats();
        static final DummyApdexStat apdexStat = new DummyApdexStat();

        public Map<String, StatsBase> getStatsMap() {
            return statsMap;
        }

        public Stats getStats(String metricName) {
            return stat;
        }

        public ResponseTimeStats getResponseTimeStats(String metric) {
            return responseTimeStat;
        }

        public void recordEmptyStats(String metricName) {
        }

        public ApdexStats getApdexStats(String metricName) {
            return apdexStat;
        }

        public void mergeStats(SimpleStatsEngine other) {
        }

        public void clear() {
        }

        public int getSize() {
            return 0;
        }

        public List<MetricData> getMetricData(Normalizer metricNormalizer, MetricIdRegistry metricIdRegistry,
                                              String scope) {
            return Collections.emptyList();
        }

        public String toString() {
            return "";
        }
    }

    static final class DummyTransactionStats extends TransactionStats {
        public static final TransactionStats INSTANCE = new DummyTransactionStats();
        static final SimpleStatsEngine stats = new DummySimpleStatsEngine();

        public SimpleStatsEngine getUnscopedStats() {
            return stats;
        }

        public SimpleStatsEngine getScopedStats() {
            return stats;
        }

        public int getSize() {
            return 0;
        }

        public String toString() {
            return "";
        }
    }

    static final class DummyTransactionActivity extends TransactionActivity {
        public static final TransactionActivity INSTANCE = new DummyTransactionActivity();

        public Object getContext() {
            return null;
        }

        public void setContext(Object context) {
        }

        public TransactionStats getTransactionStats() {
            return DummyTransactionStats.INSTANCE;
        }

        public List<Tracer> getTracers() {
            return Collections.emptyList();
        }

        public long getTotalCpuTime() {
            return 0L;
        }

        public void setToIgnore() {
        }

        void setOwningTransactionIsIgnored(boolean newState) {
        }

        public Tracer tracerStarted(Tracer tracer) {
            return tracer;
        }

        public void tracerFinished(Tracer tracer, int opcode) {
        }

        public boolean isStarted() {
            return true;
        }

        public boolean isFlyweight() {
            return false;
        }

        public void recordCpu() {
        }

        public void addTracer(Tracer tracer) {
        }

        public boolean checkTracerStart() {
            return false;
        }

        public Tracer getLastTracer() {
            return null;
        }

        public TracedMethod startFlyweightTracer() {
            return null;
        }

        public void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
                                          String methodName, String methodDesc, String metricName,
                                          String[] rollupMetricNames) {
        }

        public void startAsyncActivity(Object context, Transaction transaction, int activityId, Tracer parentTracer) {
        }

        public Tracer getRootTracer() {
            return null;
        }

        public TransactionCache getTransactionCache() {
            return Transaction.getOrCreateDummyTransaction().getTransactionCache();
        }

        public Transaction getTransaction() {
            return Transaction.getOrCreateDummyTransaction();
        }

        public int hashCode() {
            return 0;
        }
    }

    static final class DummyCrossProcessState implements CrossProcessTransactionState {
        public static final CrossProcessTransactionState INSTANCE = new DummyCrossProcessState();

        public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        }

        public void processOutboundResponseHeaders(OutboundHeaders outboundHeaders, long contentLength) {
        }

        public void processInboundResponseHeaders(InboundHeaders inboundHeaders, TracedMethod tracer, String host,
                                                  String uri, boolean addRollupMetric) {
        }

        public String getRequestMetadata() {
            return null;
        }

        public void processRequestMetadata(String requestMetadata) {
        }

        public String getResponseMetadata() {
            return null;
        }

        public void processResponseMetadata(String responseMetadata) {
        }

        public void writeResponseHeaders() {
        }

        public String getTripId() {
            return "";
        }

        public int generatePathHash() {
            return 0;
        }

        public String getAlternatePathHashes() {
            return "";
        }
    }

    static final class DummyDatabaseStatementParser implements DatabaseStatementParser {
        static final DatabaseStatementParser INSTANCE = new DummyDatabaseStatementParser();

        private static final ParsedDatabaseStatement parsedDatabaseStatement =
                new ParsedDatabaseStatement(null, null, false);

        public ParsedDatabaseStatement getParsedDatabaseStatement(String statement,
                                                                  ResultSetMetaData resultSetMetaData) {
            return parsedDatabaseStatement;
        }
    }

    static final class DummySet<E> implements Set<E> {
        private final Set<E> object = new HashSet();

        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean contains(Object o) {
            return false;
        }

        public Iterator<E> iterator() {
            return object.iterator();
        }

        public Object[] toArray() {
            return object.toArray();
        }

        public <T> T[] toArray(T[] a) {
            return object.toArray(a);
        }

        public boolean add(E e) {
            return false;
        }

        public boolean remove(Object o) {
            return false;
        }

        public boolean containsAll(Collection<?> c) {
            return false;
        }

        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        public boolean retainAll(Collection<?> c) {
            return false;
        }

        public boolean removeAll(Collection<?> c) {
            return false;
        }

        public void clear() {
        }
    }

    static final class DummyMap<K, V> implements Map<K, V> {
        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean containsKey(Object key) {
            return false;
        }

        public boolean containsValue(Object value) {
            return false;
        }

        public V get(Object key) {
            return null;
        }

        public V put(K key, V value) {
            return null;
        }

        public V remove(Object key) {
            return null;
        }

        public void putAll(Map<? extends K, ? extends V> m) {
        }

        public void clear() {
        }

        public Set<K> keySet() {
            return Collections.emptySet();
        }

        public Collection<V> values() {
            return Collections.emptySet();
        }

        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    final class DummyInsights implements Insights {
        DummyInsights() {
        }

        public void recordCustomEvent(String eventType, Map<String, Object> attributes) {
        }
    }
}