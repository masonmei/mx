//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.newrelic.agent.ThreadService.AgentThread;
import com.newrelic.agent.application.ApplicationNamingPolicy;
import com.newrelic.agent.application.HigherPriorityApplicationNamingPolicy;
import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.application.SameOrHigherPriorityApplicationNamingPolicy;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.agent.browser.BrowserTransactionState;
import com.newrelic.agent.browser.BrowserTransactionStateImpl;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.CachingDatabaseStatementParser;
import com.newrelic.agent.database.DatabaseStatementParser;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.messaging.MessagingUtil;
import com.newrelic.agent.normalization.Normalizer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.servlet.ServletUtils;
import com.newrelic.agent.sql.SqlTracerListener;
import com.newrelic.agent.stats.AbstractMetricAggregator;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.trace.TransactionGuidFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TransactionActivityInitiator;
import com.newrelic.agent.transaction.ConnectionCache;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.transaction.TransactionCache;
import com.newrelic.agent.transaction.TransactionCounts;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.transaction.TransactionTimer;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.TransactionNamePriority;

public class Transaction implements ITransaction {
    protected static final WebResponse DEFAULT_RESPONSE;
    static final ClassMethodSignature REQUEST_INITIALIZED_CLASS_SIGNATURE =
            new ClassMethodSignature("javax.servlet.ServletRequestListener", "requestInitialized",
                                            "(Ljavax/servlet/ServletRequestEvent;)V");
    static final int REQUEST_INITIALIZED_CLASS_SIGNATURE_ID;
    private static final String THREAD_ASSERTION_FAILURE = "Thread assertion failed!";
    private static final ThreadLocal<Transaction> transactionHolder;
    private static final Request DUMMY_REQUEST;
    private static final Response DUMMY_RESPONSE;
    private static final int REQUEST_TRACER_FLAGS = 14;
    private static DummyTransaction dummyTransaction;

    static {
        REQUEST_INITIALIZED_CLASS_SIGNATURE_ID = ClassMethodSignatures.get().add(REQUEST_INITIALIZED_CLASS_SIGNATURE);
        transactionHolder = new ThreadLocal() {
            public void remove() {
                ServiceFactory.getTransactionService().removeTransaction();
                super.remove();
            }

            public void set(Transaction value) {
                super.set(value);
                ServiceFactory.getTransactionService().addTransaction(value);
            }
        };
        DEFAULT_RESPONSE = new WebResponse() {
            public void setStatusMessage(String message) {
            }

            public void setStatus(int statusCode) {
            }

            public int getStatus() {
                return 0;
            }

            public String getStatusMessage() {
                return null;
            }

            public void freezeStatus() {
            }
        };
        DUMMY_REQUEST = new Request() {
            public String[] getParameterValues(String name) {
                return null;
            }

            public Enumeration<?> getParameterNames() {
                return null;
            }

            public Object getAttribute(String name) {
                return null;
            }

            public String getRequestURI() {
                return "/";
            }

            public String getRemoteUser() {
                return null;
            }

            public String getHeader(String name) {
                return null;
            }

            public String getCookieValue(String name) {
                return null;
            }

            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }
        };
        DUMMY_RESPONSE = new Response() {
            public int getStatus() throws Exception {
                return 0;
            }

            public String getStatusMessage() throws Exception {
                return null;
            }

            public void setHeader(String name, String value) {
            }

            public String getContentType() {
                return null;
            }

            public HeaderType getHeaderType() {
                return HeaderType.HTTP;
            }
        };
    }

    private final IAgent agent;
    private final String guid;
    private final boolean ttEnabled;
    private final TransactionCounts counts;
    private final boolean autoAppNamingEnabled;
    private final boolean transactionNamingEnabled;
    private final boolean ignoreErrorPriority;
    private final AtomicReference<TransactionErrorPriority> throwablePriority = new AtomicReference();
    private final Object lock = new Object();
    private final Map<Object, TransactionActivity> runningChildren;
    private final Map<Object, TransactionActivity> finishedChildren;
    private final AtomicInteger nextActivityId = new AtomicInteger(0);
    private final AtomicReference<Transaction.AppNameAndConfig> appNameAndConfig;
    private final Map<String, Object> internalParameters;
    private final Map<String, Map<String, String>> prefixedAgentAttributes;
    private final Map<String, Object> agentAttributes;
    private final Map<String, Object> intrinsicAttributes;
    private final Map<String, Object> userAttributes;
    private final Map<String, String> errorAttributes;
    private final Insights insights;
    private final Map<Object, Tracer> contextToTracer;
    private final Map<Object, Tracer> timedOutKeys;
    private final Transaction.LegacyState legacyState;
    private final MetricAggregator metricAggregator;
    private final Object requestStateChangeLock;
    private volatile long wallClockStartTimeMs;
    private volatile long startGCTimeInMillis;
    private volatile Throwable throwable;
    private volatile boolean ignore;
    private volatile Dispatcher dispatcher;
    private volatile Tracer rootTracer;
    private volatile TransactionTimer transactionTime;
    private volatile TransactionState transactionState;
    private volatile TransactionActivity initialActivity;
    private volatile ConnectionCache connectionCache;
    private volatile PriorityTransactionName priorityTransactionName;
    private CrossProcessTransactionState crossProcessTransactionState;
    private DatabaseStatementParser databaseStatementParser;
    private String normalizedUri;
    private SqlTracerListener sqlTracerListener;
    private BrowserTransactionState browserTransactionState;
    private InboundHeaders providedHeaders;
    private InboundHeaderState inboundHeaderState;

    protected Transaction() {
        this.appNameAndConfig =
                new AtomicReference(new Transaction.AppNameAndConfig(PriorityApplicationName.NONE, (AgentConfig) null));
        this.transactionState = new TransactionStateImpl();
        this.initialActivity = null;
        this.connectionCache = null;
        this.priorityTransactionName = PriorityTransactionName.NONE;
        this.legacyState = new Transaction.LegacyState();
        this.metricAggregator = new AbstractMetricAggregator() {
            protected void doRecordResponseTimeMetric(String name, long totalTime, long exclusiveTime,
                                                      TimeUnit timeUnit) {
                Transaction.this.getTransactionActivity().getTransactionStats().getUnscopedStats()
                        .getResponseTimeStats(name).recordResponseTime(totalTime, exclusiveTime, timeUnit);
            }

            protected void doRecordMetric(String name, float value) {
                Transaction.this.getTransactionActivity().getTransactionStats().getUnscopedStats().getStats(name)
                        .recordDataPoint(value);
            }

            protected void doIncrementCounter(String name, int count) {
                Transaction.this.getTransactionActivity().getTransactionStats().getUnscopedStats().getStats(name)
                        .incrementCallCount(count);
            }
        };
        this.requestStateChangeLock = new Object();
        Agent.LOG.log(Level.FINE, "create Transaction {0}", new Object[] {this});
        if (Agent.LOG.isFinestEnabled() && Agent.isDebugEnabled()) {
            Agent.LOG.log(Level.FINEST, "backtrace: {0}",
                                 new Object[] {Arrays.toString(Thread.currentThread().getStackTrace())});
        }

        AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
        this.agent = ServiceFactory.getAgent();
        this.guid = TransactionGuidFactory.generateGuid();
        this.autoAppNamingEnabled = defaultConfig.isAutoAppNamingEnabled();
        this.transactionNamingEnabled = this.initializeTransactionNamingEnabled(defaultConfig);
        this.ignoreErrorPriority =
                ((Boolean) defaultConfig.getValue("error_collector.ignoreErrorPriority", Boolean.TRUE)).booleanValue();
        TransactionTraceService ttService = ServiceFactory.getTransactionTraceService();
        this.ttEnabled = ttService.isEnabled();
        this.counts = new TransactionCounts(defaultConfig);
        MapMaker factory = (new MapMaker()).initialCapacity(8).concurrencyLevel(4);
        this.internalParameters = new LazyMapImpl(factory);
        this.prefixedAgentAttributes = new LazyMapImpl(factory);
        this.agentAttributes = new LazyMapImpl(factory);
        this.intrinsicAttributes = new LazyMapImpl(factory);
        this.userAttributes = new LazyMapImpl(factory);
        this.errorAttributes = new LazyMapImpl(factory);
        this.insights = ServiceFactory.getServiceManager().getInsights().getTransactionInsights(defaultConfig);
        this.contextToTracer = new LazyMapImpl((new MapMaker()).initialCapacity(25).concurrencyLevel(16));
        this.timedOutKeys = new LazyMapImpl(factory);
        this.runningChildren = new LazyMapImpl(factory);
        this.finishedChildren = new LazyMapImpl(factory);
    }

    private static long getGCTime() {
        long gcTime = 0L;

        GarbageCollectorMXBean gcBean;
        for (Iterator i$ = ManagementFactory.getGarbageCollectorMXBeans().iterator(); i$.hasNext();
             gcTime += gcBean.getCollectionTime()) {
            gcBean = (GarbageCollectorMXBean) i$.next();
        }

        return gcTime;
    }

    static InboundHeaders getRequestHeaders(Transaction tx) {
        return tx.dispatcher != null && tx.dispatcher.getRequest() != null
                       ? new DeobfuscatedInboundHeaders(tx.dispatcher.getRequest(),
                                                               tx.getCrossProcessConfig().getEncodingKey()) : null;
    }

    public static void clearTransaction() {
        Transaction tx = (Transaction) transactionHolder.get();
        if (tx != null) {
            tx.legacyState.boundThreads.remove(Long.valueOf(Thread.currentThread().getId()));
        }

        transactionHolder.remove();
        TransactionActivity.clear();
    }

    public static Transaction getTransaction() {
        return getTransaction(true);
    }

    public static void setTransaction(Transaction tx) {
        tx.legacyState.boundThreads.add(Long.valueOf(Thread.currentThread().getId()));
        TransactionActivity.set(tx.initialActivity);
        transactionHolder.set(tx);
    }

    static Transaction getTransaction(boolean createIfNotExists) {
        Transaction tx = (Transaction) transactionHolder.get();
        if (tx == null && createIfNotExists && !(Thread.currentThread() instanceof AgentThread)) {
            if (ServiceFactory.getServiceManager().getCircuitBreakerService().isTripped()) {
                return getOrCreateDummyTransaction();
            }

            try {
                tx = new Transaction();
                tx.postConstruct();
                ServiceFactory.getTransactionService().addTransaction(tx);
                tx.legacyState.boundThreads.add(Long.valueOf(Thread.currentThread().getId()));
                transactionHolder.set(tx);
            } catch (RuntimeException var3) {
                Agent.LOG.log(Level.FINEST, var3, "while creating Transaction", new Object[0]);
                TransactionActivity.clear();
                throw var3;
            }
        }

        return tx;
    }

    protected static synchronized Transaction getOrCreateDummyTransaction() {
        if (dummyTransaction == null) {
            dummyTransaction = new DummyTransaction();
        }

        return dummyTransaction;
    }

    public static boolean isDummyRequest(Request request) {
        return request == DUMMY_REQUEST;
    }

    private static String stripLeadingForwardSlash(String appName) {
        String FORWARD_SLASH = "/";
        return appName.length() > 1 && appName.startsWith("/") ? appName.substring(1, appName.length()) : appName;
    }

    private void postConstruct() {
        TransactionActivity txa = TransactionActivity.create(this, this.nextActivityId.getAndIncrement());
        txa.setContext(txa);
        this.initialActivity = txa;
        this.legacyState.boundThreads.add(Long.valueOf(Thread.currentThread().getId()));
        Object var2 = this.lock;
        synchronized(this.lock) {
            this.runningChildren.put(txa.getContext(), txa);
        }
    }

    private boolean initializeTransactionNamingEnabled(AgentConfig config) {
        if (!config.isAutoTransactionNamingEnabled()) {
            return false;
        } else {
            IRPMService rpmService = this.getRPMService();
            if (rpmService == null) {
                return true;
            } else {
                String transactionNamingScheme = this.getRPMService().getTransactionNamingScheme();
                return "framework" != transactionNamingScheme;
            }
        }
    }

    public MetricAggregator getMetricAggregator() {
        return this.metricAggregator;
    }

    public IAgent getAgent() {
        return this.agent;
    }

    public Object getLock() {
        return this.lock;
    }

    public String getGuid() {
        return this.guid;
    }

    public AgentConfig getAgentConfig() {
        AgentConfig config = null;

        do {
            Transaction.AppNameAndConfig nc = (Transaction.AppNameAndConfig) this.appNameAndConfig.get();
            config = nc.config;
            if (config == null) {
                config = ServiceFactory.getConfigService().getAgentConfig(nc.name.getName());
                if (!this.appNameAndConfig.compareAndSet(nc, new Transaction.AppNameAndConfig(nc.name, config))) {
                    config = null;
                }
            }
        } while (config == null);

        return config;
    }

    public long getWallClockStartTimeMs() {
        this.captureWallClockStartTime();
        return this.wallClockStartTimeMs;
    }

    private void captureWallClockStartTime() {
        if (this.wallClockStartTimeMs == 0L) {
            this.wallClockStartTimeMs = System.currentTimeMillis();
        }

    }

    public Map<String, Object> getInternalParameters() {
        return this.internalParameters;
    }

    public Map<String, Map<String, String>> getPrefixedAgentAttributes() {
        return this.prefixedAgentAttributes;
    }

    public Map<String, Object> getUserAttributes() {
        return this.userAttributes;
    }

    public Map<String, Object> getAgentAttributes() {
        return this.agentAttributes;
    }

    public Map<String, Object> getIntrinsicAttributes() {
        return this.intrinsicAttributes;
    }

    public Map<String, String> getErrorAttributes() {
        return this.errorAttributes;
    }

    public Insights getInsightsData() {
        return this.insights;
    }

    public TransactionTracerConfig getTransactionTracerConfig() {
        return this.dispatcher == null ? this.getAgentConfig().getTransactionTracerConfig()
                       : this.dispatcher.getTransactionTracerConfig();
    }

    public CrossProcessConfig getCrossProcessConfig() {
        return this.getAgentConfig().getCrossProcessConfig();
    }

    public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category,
                                      String... parts) {
        return this.setTransactionName(com.newrelic.agent.bridge.TransactionNamePriority.convert(namePriority),
                                              override, category, parts);
    }

    public boolean setTransactionName(com.newrelic.agent.bridge.TransactionNamePriority namePriority, boolean override,
                                      String category, String... parts) {
        return this.getRootTransaction().doSetTransactionName(namePriority, override, category, parts);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public boolean isTransactionNameSet() {
        return this.getRootTransaction().getPriorityTransactionName().getPriority()
                       .isGreaterThan(com.newrelic.agent.bridge.TransactionNamePriority.NONE);
    }

    private boolean doSetTransactionName(com.newrelic.agent.bridge.TransactionNamePriority namePriority,
                                         boolean override, String category, String... parts) {
        if (namePriority.isLessThan(com.newrelic.agent.bridge.TransactionNamePriority.CUSTOM_HIGH)
                    && !this.isTransactionNamingEnabled()) {
            return false;
        } else {
            String name = Strings.join('/', parts);
            if (this.dispatcher == null) {
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.finest(MessageFormat
                                             .format("Unable to set the transaction name to \"{0}\" - no transaction",
                                                            new Object[] {name}));
                }

                return false;
            } else {
                TransactionNamingPolicy policy =
                        override ? TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy()
                                : TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
                return policy.setTransactionName(this, name, category, namePriority);
            }
        }
    }

    public PriorityTransactionName getPriorityTransactionName() {
        return this.priorityTransactionName;
    }

    public void freezeTransactionName() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            if (!this.priorityTransactionName.isFrozen()) {
                this.dispatcher.setTransactionName();
                this.renameTransaction();
                this.priorityTransactionName = this.priorityTransactionName.freeze();
            }
        }
    }

    private void renameTransaction() {
        if (Agent.LOG.isFinestEnabled()) {
            this.threadAssertion();
        }

        String appName = this.getApplicationName();
        Normalizer metricDataNormalizer = ServiceFactory.getNormalizationService().getMetricNormalizer(appName);
        String txName = metricDataNormalizer.normalize(this.priorityTransactionName.getName());
        Normalizer txNormalizer = ServiceFactory.getNormalizationService().getTransactionNormalizer(appName);
        txName = txNormalizer.normalize(txName);
        if (txName == null) {
            this.setIgnore(true);
        } else {
            if (!txName.equals(this.priorityTransactionName.getName())) {
                this.setPriorityTransactionNameLocked(PriorityTransactionName
                                                              .create(txName, this.isWebTransaction() ? "Web" : "Other",
                                                                             com.newrelic.agent.bridge
                                                                                     .TransactionNamePriority
                                                                                     .REQUEST_URI));
            }

        }
    }

    public boolean conditionalSetPriorityTransactionName(TransactionNamingPolicy policy, String name, String category,
                                                         com.newrelic.agent.bridge.TransactionNamePriority priority) {
        Object var5 = this.lock;
        synchronized(this.lock) {
            if (policy.canSetTransactionName(this, priority)) {
                Agent.LOG.log(Level.FINER, "Setting transaction name to \"{0}\" for transaction {1}",
                                     new Object[] {name, this});
                return this.setPriorityTransactionNameLocked(policy.getPriorityTransactionName(this, name, category,
                                                                                                      priority));
            } else {
                Agent.LOG.log(Level.FINER,
                                     "Not setting the transaction name to  \"{0}\" for transaction {1}: a higher "
                                             + "priority name is already in place. Current transaction name is {2}",
                                     new Object[] {name, this, this.getTransactionName()});
                return false;
            }
        }
    }

    public boolean setPriorityTransactionName(PriorityTransactionName ptn) {
        Object var2 = this.lock;
        synchronized(this.lock) {
            return this.setPriorityTransactionNameLocked(ptn);
        }
    }

    private boolean setPriorityTransactionNameLocked(PriorityTransactionName ptn) {
        if (Agent.LOG.isFinestEnabled()) {
            this.threadAssertion();
        }

        if (ptn == null) {
            return false;
        } else {
            this.priorityTransactionName = ptn;
            return true;
        }
    }

    public SqlTracerListener getSqlTracerListener() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            if (this.sqlTracerListener == null) {
                String appName = this.getApplicationName();
                this.sqlTracerListener = ServiceFactory.getSqlTraceService().getSqlTracerListener(appName);
            }

            return this.sqlTracerListener;
        }
    }

    public TransactionCache getTransactionCache() {
        return this.getTransactionActivity().getTransactionCache();
    }

    public ConnectionCache getConnectionCache() {
        if (this.connectionCache == null) {
            Object var1 = this.lock;
            synchronized(this.lock) {
                if (this.connectionCache == null) {
                    this.connectionCache = new ConnectionCache();
                }
            }
        }

        return this.connectionCache;
    }

    public boolean isStarted() {
        return this.getDispatcher() != null;
    }

    public boolean isFinished() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            return this.isStarted() && this.runningChildren.isEmpty() && this.contextToTracer.isEmpty();
        }
    }

    public boolean isInProgress() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            return this.isStarted() && (!this.runningChildren.isEmpty() || !this.contextToTracer.isEmpty());
        }
    }

    public Dispatcher getDispatcher() {
        return this.dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        Agent.LOG.log(Level.FINER, "Set dispatcher {0} for transaction {1}", new Object[] {dispatcher, this});
        this.dispatcher = dispatcher;
    }

    public long getExternalTime() {
        return this.dispatcher instanceof WebRequestDispatcher ? ((WebRequestDispatcher) this.dispatcher).getQueueTime()
                       : 0L;
    }

    public Tracer getRootTracer() {
        return this.rootTracer;
    }

    public List<Tracer> getAllTracers() {
        this.transactionState.mergeAsyncTracers();
        return this.getTracers();
    }

    public List<Tracer> getTracers() {
        return new TracerList(this.getRootTracer(), this.getFinishedChildren());
    }

    public TransactionActivity getTransactionActivity() {
        if (this.legacyState.boundThreads.size() == 0) {
            return this.initialActivity;
        } else {
            TransactionActivity result = TransactionActivity.get();
            if (result == null) {
                throw new IllegalStateException("TransactionActivity is gone");
            } else {
                return result;
            }
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public TransactionActivity getInitialTransactionActivity() {
        return this.initialActivity;
    }

    void activityStarted(TransactionActivity activity) {
        Agent.LOG.log(Level.FINER, "activity {0} starting", new Object[] {activity});
        this.startTransactionIfBeginning(activity.getRootTracer());
    }

    public void startTransactionIfBeginning(Tracer tracer) {
        if (tracer instanceof TransactionActivityInitiator) {
            Agent.LOG.log(Level.FINER, "Starting transaction {0}", new Object[] {this});
            this.captureWallClockStartTime();
            if (ServiceFactory.getTransactionTraceService().isEnabled()) {
                AgentConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();
                this.startGCTimeInMillis =
                        defaultConfig.getTransactionTracerConfig().isGCTimeEnabled() ? getGCTime() : -1L;
            } else {
                this.startGCTimeInMillis = -1L;
            }

            if (this.rootTracer == null) {
                this.rootTracer = tracer;
            }

            if (this.transactionTime == null) {
                this.transactionTime = new TransactionTimer(tracer.getStartTime());
                Agent.LOG.log(Level.FINER, "Set timer for transaction {0}", new Object[] {this});
            }

            if (this.dispatcher == null) {
                this.setDispatcher(((TransactionActivityInitiator) tracer).createDispatcher());
            }
        }

    }

    public TransactionTimer getTransactionTimer() {
        return this.transactionTime;
    }

    private void finishTransaction() {
        String requestURI = this.dispatcher == null ? "No Dispatcher Defined" : this.dispatcher.getUri();
        IRPMService rpmService = this.getRPMService();
        this.beforeSendResponseHeaders();
        this.freezeTransactionName();
        if (!this.ignore && !this.finishedChildren.isEmpty()) {
            if (this.isAsyncTransaction()) {
                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    String transactionStats1 =
                            MessageFormat.format("Async transaction {1} finished {0}", new Object[] {requestURI, this});
                    Agent.LOG.finest(transactionStats1);
                }

            } else {
                TransactionStats transactionStats = this.transactionFinishedActivityMerging();
                this.recordFinalGCTime(transactionStats);
                this.addUnStartedAsyncKeys(transactionStats);
                String txName = this.priorityTransactionName.getName();
                this.dispatcher.transactionFinished(txName, transactionStats);
                if (Agent.LOG.isFinerEnabled()) {
                    Agent.LOG.log(Level.FINER, "Transaction {2} finished {0}ms {1}",
                                         new Object[] {Long.valueOf(this.transactionTime
                                                                            .getResponseTimeInMilliseconds()),
                                                              requestURI, this});
                }

                if (ServiceFactory.getServiceManager().isStarted()) {
                    if (Agent.LOG.isFinerEnabled()) {
                        Agent.LOG
                                .log(Level.FINER, "Transaction name for {0} is {1}", new Object[] {requestURI, txName});
                        if (this.isAutoAppNamingEnabled()) {
                            Agent.LOG.log(Level.FINER, "Application name for {0} is {1}",
                                                 new Object[] {txName, rpmService.getApplicationName()});
                        }
                    }

                    TransactionTracerConfig ttConfig = this.getTransactionTracerConfig();
                    TransactionCounts rootCounts = this.getTransactionCounts();
                    if (rootCounts.isOverTracerSegmentLimit()) {
                        this.getIntrinsicAttributes()
                                .put("segment_clamp", Integer.valueOf(rootCounts.getSegmentCount()));
                    }

                    if (rootCounts.isOverTransactionSize()) {
                        this.getIntrinsicAttributes().put("size_limit", "The transaction size limit was reached");
                    }

                    int count = rootCounts.getStackTraceCount();
                    if (count >= ttConfig.getMaxStackTraces()) {
                        this.getIntrinsicAttributes().put("stack_trace_clamp", Integer.valueOf(count));
                    }

                    count = rootCounts.getExplainPlanCount();
                    if (count >= ttConfig.getMaxExplainPlans()) {
                        this.getIntrinsicAttributes().put("explain_plan_clamp", Integer.valueOf(count));
                    }

                    String referrerGuid;
                    if (this.getInboundHeaderState().isTrustedCatRequest()) {
                        referrerGuid = this.getInboundHeaderState().getClientCrossProcessId();
                        this.getIntrinsicAttributes().put("client_cross_process_id", referrerGuid);
                    }

                    referrerGuid = this.getInboundHeaderState().getReferrerGuid();
                    if (referrerGuid != null) {
                        this.getIntrinsicAttributes().put("referring_transaction_guid", referrerGuid);
                    }

                    String tripId = this.getCrossProcessTransactionState().getTripId();
                    if (tripId != null) {
                        this.getIntrinsicAttributes().put("trip_id", tripId);
                        int displayHost = this.getCrossProcessTransactionState().generatePathHash();
                        this.getIntrinsicAttributes().put("path_hash", ServiceUtils.intToHexString(displayHost));
                    }

                    if (this.isSynthetic()) {
                        Agent.LOG.log(Level.FINEST, "Completing Synthetics transaction for monitor {0}",
                                             new Object[] {this.getInboundHeaderState().getSyntheticsMonitorId()});
                        this.getIntrinsicAttributes()
                                .put("synthetics_resource_id", this.getInboundHeaderState().getSyntheticsResourceId());
                        this.getIntrinsicAttributes()
                                .put("synthetics_monitor_id", this.getInboundHeaderState().getSyntheticsMonitorId());
                        this.getIntrinsicAttributes()
                                .put("synthetics_job_id", this.getInboundHeaderState().getSyntheticsJobId());
                    }

                    String displayHost1 =
                            (String) this.getAgentConfig().getValue("process_host.display_name", (Object) null);
                    if (displayHost1 != null) {
                        this.getAgentAttributes().put("host.displayName", displayHost1);
                    }

                    String instanceName = ServiceFactory.getEnvironmentService().getEnvironment().getAgentIdentity()
                                                  .getInstanceName();
                    if (instanceName != null) {
                        this.getAgentAttributes().put("process.instanceName", instanceName);
                    }

                    this.getAgentAttributes().put("jvm.thread_name", Thread.currentThread().getName());
                    TransactionData transactionData = new TransactionData(this, rootCounts.getTransactionSize());
                    ServiceFactory.getTransactionService().processTransaction(transactionData, transactionStats);
                }
            }
        } else {
            Agent.LOG.log(Level.FINE, "Ignoring transaction {0}", new Object[] {this});
        }
    }

    private TransactionStats transactionFinishedActivityMerging() {
        TransactionStats transactionStats = null;
        long totalCpuTime;
        if (this.isTransactionTraceEnabled() && this.getRunningDurationInNanos() > this.getTransactionTracerConfig()
                                                                                           .getTransactionThresholdInNanos()) {
            Object i$ = this.getIntrinsicAttributes().remove("cpu_time");
            if (i$ != null && i$ instanceof Long) {
                totalCpuTime = ((Long) i$).longValue();
            } else {
                totalCpuTime = 0L;
            }
        } else {
            totalCpuTime = -1L;
        }

        Iterator i$1 = this.getFinishedChildren().iterator();

        while (i$1.hasNext()) {
            TransactionActivity kid = (TransactionActivity) i$1.next();
            if (transactionStats == null) {
                transactionStats = kid.getTransactionStats();
            } else {
                TransactionStats rootTracer = kid.getTransactionStats();
                transactionStats.getScopedStats().mergeStats(rootTracer.getScopedStats());
                transactionStats.getUnscopedStats().mergeStats(rootTracer.getUnscopedStats());
            }

            if (kid.getRootTracer() != null) {
                Tracer rootTracer1 = kid.getRootTracer();
                this.transactionTime.incrementTransactionTotalTime(rootTracer1.getDuration());
                this.transactionTime.setTransactionEndTimeIfLonger(rootTracer1.getEndTime());
                if (Agent.LOG.isFinestEnabled()) {
                    Map tracerAtts = rootTracer1.getAttributes();
                    if (tracerAtts != null && !tracerAtts.isEmpty()) {
                        Agent.LOG.log(Level.FINEST, "Tracer Attributes for {0} are {1}",
                                             new Object[] {rootTracer1, tracerAtts});
                    }
                }
            }

            if (totalCpuTime > -1L) {
                long tempCpuTime = kid.getTotalCpuTime() > -1L ? kid.getTotalCpuTime() : -1L;
                if (tempCpuTime == -1L) {
                    totalCpuTime = -1L;
                } else {
                    totalCpuTime += tempCpuTime;
                }
            }
        }

        if (totalCpuTime > 0L) {
            this.getIntrinsicAttributes().put("cpu_time", Long.valueOf(totalCpuTime));
        }

        return transactionStats;
    }

    public synchronized void addTotalCpuTimeForLegacy(long time) {
        Object val = this.getIntrinsicAttributes().remove("cpu_time");
        long totalCpuTime;
        if (val != null && val instanceof Long) {
            totalCpuTime = ((Long) val).longValue();
        } else {
            totalCpuTime = 0L;
        }

        if (totalCpuTime != -1L) {
            totalCpuTime += time;
        }

        this.getIntrinsicAttributes().put("cpu_time", Long.valueOf(totalCpuTime));
    }

    public void recordFinalGCTime(TransactionStats stats) {
        if (this.isTransactionTraceEnabled() && this.getRunningDurationInNanos() > this.getTransactionTracerConfig()
                                                                                           .getTransactionThresholdInNanos()) {
            Long totalGCTime = (Long) this.getIntrinsicAttributes().get("gc_time");
            if (totalGCTime == null && this.startGCTimeInMillis > -1L) {
                long gcTime = getGCTime();
                if (gcTime != this.startGCTimeInMillis) {
                    totalGCTime = Long.valueOf(gcTime - this.startGCTimeInMillis);
                    this.getIntrinsicAttributes().put("gc_time", totalGCTime);
                    stats.getUnscopedStats().getResponseTimeStats("GC/cumulative")
                            .recordResponseTime(totalGCTime.longValue(), TimeUnit.MILLISECONDS);
                }
            }
        }

    }

    private void addUnStartedAsyncKeys(TransactionStats stats) {
        if (!this.timedOutKeys.isEmpty()) {
            stats.getUnscopedStats().getStats("Supportability/Timeout/startAsyncNotCalled")
                    .setCallCount(this.timedOutKeys.size());
        }

        if (this.isTransactionTraceEnabled()) {
            Iterator i$ = this.timedOutKeys.entrySet().iterator();

            while (i$.hasNext()) {
                Entry current = (Entry) i$.next();
                Object val = ((Tracer) current.getValue()).getAttribute("unstarted_async_activity");
                Object keys;
                if (val == null) {
                    keys = Maps.newHashMap();
                } else {
                    keys = (Map) val;
                }

                String classType = current.getKey().getClass().toString();
                Integer count = (Integer) ((Map) keys).get(classType);
                if (count == null) {
                    count = Integer.valueOf(1);
                } else {
                    count = Integer.valueOf(count.intValue() + 1);
                }

                ((Map) keys).put(classType, count);
                ((Tracer) current.getValue()).setAttribute("unstarted_async_activity", keys);
            }
        }

    }

    public boolean isTransactionTraceEnabled() {
        return this.ttEnabled;
    }

    public boolean isAutoAppNamingEnabled() {
        return this.autoAppNamingEnabled;
    }

    public boolean isTransactionNamingEnabled() {
        return this.transactionNamingEnabled;
    }

    public boolean isWebTransaction() {
        return this.dispatcher != null && this.dispatcher.isWebTransaction();
    }

    public boolean isAsyncTransaction() {
        return this.dispatcher != null && this.dispatcher.isAsyncTransaction();
    }

    public boolean isSynthetic() {
        return this.getInboundHeaderState().isTrustedSyntheticsRequest();
    }

    public void provideHeaders(InboundHeaders headers) {
        if (headers != null) {
            String encodingKey = this.getCrossProcessConfig().getEncodingKey();
            this.provideRawHeaders(new DeobfuscatedInboundHeaders(headers, encodingKey));
        }

    }

    public void provideRawHeaders(InboundHeaders headers) {
        if (headers != null) {
            Object var2 = this.lock;
            synchronized(this.lock) {
                this.providedHeaders = headers;
            }
        }

    }

    public InboundHeaderState getInboundHeaderState() {
        Transaction tx = this.getRootTransaction();
        Object var2 = tx.lock;
        synchronized(tx.lock) {
            if (tx.inboundHeaderState == null) {
                InboundHeaders requestHeaders = getRequestHeaders(tx);
                if (requestHeaders == null) {
                    requestHeaders = this.providedHeaders == null ? null : this.providedHeaders;
                }

                try {
                    tx.inboundHeaderState = new InboundHeaderState(tx, requestHeaders);
                } catch (RuntimeException var6) {
                    Agent.LOG.log(Level.FINEST, "Unable to parse inbound headers", var6);
                    tx.inboundHeaderState = new InboundHeaderState(tx, (InboundHeaders) null);
                }
            }

            return tx.inboundHeaderState;
        }
    }

    public IRPMService getRPMService() {
        return ServiceFactory.getRPMServiceManager().getOrCreateRPMService(this.getPriorityApplicationName());
    }

    /**
     * @deprecated
     */
    @Deprecated
    public String getNormalizedUri() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            return this.normalizedUri;
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setNormalizedUri(String normalizedUri) {
        Object var2 = this.lock;
        synchronized(this.lock) {
            if (normalizedUri != null && normalizedUri.length() != 0) {
                TransactionNamingPolicy policy =
                        TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
                if (Agent.LOG.isLoggable(Level.FINER) && policy.canSetTransactionName(this,
                                                                                             com.newrelic.agent
                                                                                                     .bridge
                                                                                                     .TransactionNamePriority.CUSTOM_HIGH)) {
                    String msg = MessageFormat
                                         .format("Setting transaction name to normalized URI \"{0}\" for transaction "
                                                         + "{1}",
                                                        new Object[] {normalizedUri, this});
                    Agent.LOG.finer(msg);
                }

                policy.setTransactionName(this, normalizedUri, "NormalizedUri",
                                                 com.newrelic.agent.bridge.TransactionNamePriority.CUSTOM_HIGH);
                this.normalizedUri = normalizedUri;
            }
        }
    }

    public Throwable getReportError() {
        return ServletUtils.getReportError(this.throwable);
    }

    public int getStatus() {
        return this.getWebResponse().getStatus();
    }

    public String getStatusMessage() {
        return this.getWebResponse().getStatusMessage();
    }

    public void freezeStatus() {
        this.getWebResponse().freezeStatus();
    }

    public void setThrowable(Throwable throwable, TransactionErrorPriority priority) {
        if (throwable != null) {
            if (TransactionActivity.get() != this.initialActivity && priority != TransactionErrorPriority.API) {
                if (Agent.LOG.isFinerEnabled()) {
                    Agent.LOG.log(Level.FINER,
                                         "Non-API call to setThrowable from asynchronous activity ignored: {0} with "
                                                 + "priority {1}",
                                         new Object[] {throwable, priority});
                }

            } else {
                if (Agent.LOG.isFinerEnabled() && !this.ignoreErrorPriority) {
                    Agent.LOG.log(Level.FINER,
                                         "Attempting to set throwable in transaction: {0} having priority {1} with "
                                                 + "priority {2}",
                                         new Object[] {throwable.getClass().getName(), this.throwablePriority,
                                                              priority});
                }

                if (this.ignoreErrorPriority || priority.updateCurrentPriority(this.throwablePriority)) {
                    Agent.LOG.log(Level.FINER, "Set throwable {0} in transaction {1}",
                                         new Object[] {throwable.getClass().getName(), this});
                    this.throwable = throwable;
                }

            }
        }
    }

    public boolean isIgnore() {
        return this.ignore;
    }

    public void setIgnore(boolean ignore) {
        if (this.dispatcher != null) {
            Object var2 = this.lock;
            synchronized(this.lock) {
                this.ignore = ignore;
                Iterator i$ = this.runningChildren.values().iterator();

                TransactionActivity finishedChild;
                while (i$.hasNext()) {
                    finishedChild = (TransactionActivity) i$.next();
                    finishedChild.setOwningTransactionIsIgnored(true);
                }

                i$ = this.finishedChildren.values().iterator();

                while (i$.hasNext()) {
                    finishedChild = (TransactionActivity) i$.next();
                    finishedChild.setOwningTransactionIsIgnored(true);
                }
            }
        } else {
            Agent.LOG.log(Level.FINEST, "setIgnore called outside of an open transaction");
        }

    }

    public void ignore() {
        this.setIgnore(true);
    }

    public void ignoreApdex() {
        if (this.dispatcher != null) {
            this.dispatcher.setIgnoreApdex(true);
        } else {
            Agent.LOG.finer("ignoreApdex invoked with no transaction");
        }

    }

    public TransactionCounts getTransactionCounts() {
        return this.getRootTransaction().counts;
    }

    public boolean shouldGenerateTransactionSegment() {
        return this.ttEnabled && this.getTransactionCounts().shouldGenerateTransactionSegment();
    }

    public DatabaseStatementParser getDatabaseStatementParser() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            if (this.databaseStatementParser == null) {
                this.databaseStatementParser = this.createDatabaseStatementParser();
            }

            return this.databaseStatementParser;
        }
    }

    private DatabaseStatementParser createDatabaseStatementParser() {
        return new CachingDatabaseStatementParser(ServiceFactory.getDatabaseService().getDatabaseStatementParser());
    }

    public BrowserTransactionState getBrowserTransactionState() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            if (this.browserTransactionState == null) {
                this.browserTransactionState = BrowserTransactionStateImpl.create(this);
            }

            return this.browserTransactionState;
        }
    }

    public CrossProcessState getCrossProcessState() {
        return this.getCrossProcessTransactionState();
    }

    public CrossProcessTransactionState getCrossProcessTransactionState() {
        Transaction tx = this.getRootTransaction();
        synchronized(tx) {
            if (tx.crossProcessTransactionState == null) {
                tx.crossProcessTransactionState = CrossProcessTransactionStateImpl.create(tx);
            }

            return tx.crossProcessTransactionState;
        }
    }

    public TransactionState getTransactionState() {
        return this.transactionState;
    }

    public void setTransactionState(TransactionState transactionState) {
        Agent.disableFastPath();
        this.transactionState = transactionState;
    }

    public Transaction getRootTransaction() {
        return this.legacyState.rootTransaction == null ? this : this.legacyState.rootTransaction;
    }

    public void setRootTransaction(Transaction tx) {
        if (this != tx) {
            this.legacyState.rootTransaction = tx;
        }

    }

    public void beforeSendResponseHeaders() {
        this.getCrossProcessTransactionState().writeResponseHeaders();
    }

    public WebResponse getWebResponse() {
        return this.dispatcher instanceof WebResponse ? (WebResponse) this.dispatcher : DEFAULT_RESPONSE;
    }

    public void setWebResponse(Response response) {
        NewRelic.getAgent().getLogger().log(Level.FINEST, "setWebResponse invoked", new Object[0]);
        if (this.dispatcher instanceof WebRequestDispatcher) {
            this.dispatcher.setResponse(response);
        }

    }

    public void convertToWebTransaction() {
        if (!this.isWebTransaction()) {
            this.setDispatcher(new WebRequestDispatcher(DUMMY_REQUEST, DUMMY_RESPONSE, this));
        }

    }

    public void requestInitialized(Request request, Response response) {
        Agent.LOG.log(Level.FINEST, "Request initialized: {0}", new Object[] {request.getRequestURI()});
        Object var3 = this.requestStateChangeLock;
        synchronized(this.requestStateChangeLock) {
            if (!this.isFinished()) {
                if (this.dispatcher == null) {
                    ExitTracer tracer = AgentBridge.instrumentation
                                                .createTracer((Object) null, REQUEST_INITIALIZED_CLASS_SIGNATURE_ID,
                                                                     (String) null, 14);
                    if (tracer != null) {
                        if (response == null) {
                            response = DUMMY_RESPONSE;
                        }

                        this.setDispatcher(new WebRequestDispatcher(request, response, this));
                    }
                } else {
                    Agent.LOG.finer("requestInitialized(): transaction already started.");
                }

            }
        }
    }

    public void requestDestroyed() {
        Agent.LOG.log(Level.FINEST, "Request destroyed");
        Object var1 = this.requestStateChangeLock;
        synchronized(this.requestStateChangeLock) {
            if (this.isInProgress()) {
                Tracer rootTracer = this.getTransactionActivity().getRootTracer();
                Tracer lastTracer = this.getTransactionActivity().getLastTracer();
                if (lastTracer != null && rootTracer == lastTracer) {
                    lastTracer.finish(177, (Object) null);
                } else {
                    Agent.LOG.log(Level.FINER, "Inconsistent state!  tracer != last tracer for {0} ({1} != {2})",
                                         new Object[] {this, rootTracer, lastTracer});
                }

            }
        }
    }

    public boolean isWebRequestSet() {
        return this.dispatcher instanceof WebRequestDispatcher ? !DUMMY_REQUEST.equals(this.dispatcher.getRequest())
                       : false;
    }

    public boolean isWebResponseSet() {
        return this.dispatcher instanceof WebRequestDispatcher ? !DUMMY_RESPONSE.equals(this.dispatcher.getResponse())
                       : false;
    }

    public void setWebRequest(Request request) {
        NewRelic.getAgent().getLogger().log(Level.FINEST, "setWebRequest invoked", new Object[0]);
        if (!(this.dispatcher instanceof WebRequestDispatcher)) {
            this.setDispatcher(new WebRequestDispatcher(request, DUMMY_RESPONSE, getTransaction()));
        } else {
            this.dispatcher.setRequest(request);
        }

    }

    public String getApplicationName() {
        return this.getPriorityApplicationName().getName();
    }

    public PriorityApplicationName getPriorityApplicationName() {
        return ((Transaction.AppNameAndConfig) this.appNameAndConfig.get()).name;
    }

    private void setPriorityApplicationName(PriorityApplicationName pan) {
        if (pan != null && !pan.equals(this.getPriorityApplicationName())) {
            Agent.LOG.log(Level.FINE, "Set application name to {0}", new Object[] {pan.getName()});
            this.appNameAndConfig.set(new Transaction.AppNameAndConfig(pan, (AgentConfig) null));
        }
    }

    public void setApplicationName(ApplicationNamePriority priority, String appName) {
        this.setApplicationName(priority, appName, false);
    }

    private void setApplicationName(ApplicationNamePriority priority, String appName, boolean override) {
        if (appName != null && appName.length() != 0) {
            Object policy = override ? SameOrHigherPriorityApplicationNamingPolicy.getInstance()
                                    : HigherPriorityApplicationNamingPolicy.getInstance();
            Object var5 = this.lock;
            synchronized(this.lock) {
                if (((ApplicationNamingPolicy) policy).canSetApplicationName(this, priority)) {
                    String name = stripLeadingForwardSlash(appName);
                    PriorityApplicationName pan = PriorityApplicationName.create(name, priority);
                    this.setPriorityApplicationName(pan);
                }

            }
        }
    }

    public long getRunningDurationInNanos() {
        return this.dispatcher == null ? 0L : this.transactionTime.getRunningDurationInNanos();
    }

    public void saveMessageParameters(Map<String, String> parameters) {
        MessagingUtil.recordParameters(this, parameters);
    }

    public boolean registerAsyncActivity(Object activityContext) {
        boolean result = false;
        Object var3 = this.lock;
        synchronized(this.lock) {
            if (this.isInProgress()) {
                Tracer t = this.getTransactionActivity().getLastTracer();
                if (t == null) {
                    Agent.LOG.log(Level.FINE,
                                         "Parent tracer not found. Not registering async activity context {0} with "
                                                 + "transaction {1}",
                                         new Object[] {activityContext, this});
                } else if (!ServiceFactory.getAsyncTxService().putIfAbsent(activityContext, this)) {
                    Agent.LOG.log(Level.FINER,
                                         "Key already in use. Not registering async activity context {0} with "
                                                 + "transaction {1}",
                                         new Object[] {activityContext, this});
                } else {
                    this.contextToTracer.put(activityContext, t);
                    Agent.LOG.log(Level.FINER, "Registering async activity context {0} with transaction {1}",
                                         new Object[] {activityContext, this});
                    result = true;
                }
            }

            return result;
        }
    }

    public boolean startAsyncActivity(Object activityContext) {
        boolean result = false;
        Object var3 = this.lock;
        synchronized(this.lock) {
            if (this.isInProgress()) {
                Transaction transaction = ServiceFactory.getAsyncTxService().extractIfPresent(activityContext);
                if (transaction == null) {
                    Agent.LOG.log(Level.FINER,
                                         "startAsyncActivity(): there is no transaction associated with context {0}",
                                         new Object[] {activityContext});
                } else if (transaction == this) {
                    Agent.LOG.log(Level.FINER, "Transaction started in current running transaction {0} for context {1}",
                                         new Object[] {transaction, activityContext});
                    this.contextToTracer.remove(activityContext);
                    if (this.isIgnore()) {
                        this.getTransactionActivity().setOwningTransactionIsIgnored(this.isIgnore());
                    }
                } else {
                    result = true;
                    this.migrate(transaction, activityContext);
                    Agent.LOG.log(Level.FINER,
                                         "startAsyncActivity(): activity {0} (context {1}) unbound from transaction "
                                                 + "{2} and bound to {3}",
                                         new Object[] {this.getTransactionActivity(), activityContext, this,
                                                              transaction});
                }
            } else {
                Agent.LOG.log(Level.FINER, "startAsyncActivity must be called within a transaction.");
            }

            return result;
        }
    }

    public void timeoutAsyncActivity(Object activityContext) {
        Object var2 = this.lock;
        synchronized(this.lock) {
            Tracer tracer = (Tracer) this.contextToTracer.remove(activityContext);
            if (tracer != null) {
                this.timedOutKeys.put(activityContext, tracer);
                this.checkFinishTransaction();
            }

        }
    }

    public boolean ignoreAsyncActivity(Object activityContext) {
        String baseMessage = "ignoreAsyncActivity({0}): {1}";
        boolean result = true;
        Object var4 = this.lock;
        synchronized(this.lock) {
            String detailMessage = null;
            Transaction tx = ServiceFactory.getAsyncTxService().extractIfPresent(activityContext);
            if (tx != null) {
                Tracer txa = (Tracer) tx.contextToTracer.remove(activityContext);
                if (txa == null) {
                    Agent.LOG.log(Level.FINER, "ignoreAsyncActivity({0}): {1}",
                                         new Object[] {activityContext, "tracer not found"});
                }

                detailMessage = "pending activity ignored.";
            } else {
                TransactionActivity txa1;
                if (this.runningChildren.containsKey(activityContext)) {
                    txa1 = (TransactionActivity) this.runningChildren.remove(activityContext);
                    txa1.setToIgnore();
                    detailMessage = "running activity ignored.";
                } else if (this.finishedChildren.containsKey(activityContext)) {
                    txa1 = (TransactionActivity) this.finishedChildren.remove(activityContext);
                    txa1.setToIgnore();
                    detailMessage = "finished activity ignored.";
                } else {
                    detailMessage = "activity not found.";
                    result = false;
                }
            }

            Agent.LOG.log(Level.FINE, "ignoreAsyncActivity({0}): {1}", new Object[] {activityContext, detailMessage});
            this.checkFinishTransaction();
            return result;
        }
    }

    private void migrate(Transaction newTrans, Object context) {
        if (Agent.LOG.isFinestEnabled()) {
            this.threadAssertion();
        }

        if (this != newTrans) {
            TransactionActivity activity = this.getTransactionActivity();
            activity.setOwningTransactionIsIgnored(newTrans.isIgnore());
            Tracer tracer = (Tracer) newTrans.contextToTracer.remove(context);
            activity.startAsyncActivity(context, newTrans, this.nextActivityId.getAndIncrement(), tracer);
            newTrans.runningChildren.put(context, activity);
            transactionHolder.set(newTrans);
            newTrans.legacyState.boundThreads.add(Long.valueOf(Thread.currentThread().getId()));
            PriorityApplicationName pan = this.getPriorityApplicationName();
            if (pan != PriorityApplicationName.NONE) {
                newTrans.setApplicationName(pan.getPriority(), pan.getName(), true);
            }

            PriorityTransactionName ptn = this.getPriorityTransactionName();
            if (ptn != PriorityTransactionName.NONE) {
                newTrans.setTransactionName(ptn.getPriority(), true, ptn.getCategory(), new String[] {ptn.getName()});
            }

            newTrans.getInternalParameters().putAll(this.getInternalParameters());
            newTrans.getPrefixedAgentAttributes().putAll(this.getPrefixedAgentAttributes());
            newTrans.getAgentAttributes().putAll(this.getAgentAttributes());
            newTrans.getIntrinsicAttributes().putAll(this.getIntrinsicAttributes());
            newTrans.getUserAttributes().putAll(this.getUserAttributes());
            newTrans.getErrorAttributes().putAll(this.getErrorAttributes());
        }
    }

    public Set<TransactionActivity> getFinishedChildren() {
        Object var1 = this.lock;
        synchronized(this.lock) {
            return new HashSet(this.finishedChildren.values());
        }
    }

    public void activityFinished(TransactionActivity activity, Tracer tracer, int opcode) {
        Agent.LOG.log(Level.FINER, "Activity {0} with context {1} finished with opcode {2} in transaction {3}.",
                             new Object[] {activity, activity.getContext(), Integer.valueOf(opcode), this});
        Object var4 = this.lock;
        synchronized(this.lock) {
            try {
                Object context = activity.getContext();
                if (this.runningChildren.remove(context) == null) {
                    Agent.LOG.log(Level.FINE,
                                         "The completing activity {0} was not in the running list for transaction {1}",
                                         new Object[] {activity, this});
                } else {
                    this.finishedChildren.put(context, activity);
                }

                this.checkFinishTransaction();
            } finally {
                this.legacyState.boundThreads.remove(Long.valueOf(Thread.currentThread().getId()));
                transactionHolder.remove();
            }

        }
    }

    public void activityFailed(TransactionActivity activity, int opcode) {
        Agent.LOG.log(Level.FINER, "activity {0} FAILED with opcode {1}",
                             new Object[] {activity, Integer.valueOf(opcode)});
        Object var3 = this.lock;
        synchronized(this.lock) {
            try {
                this.runningChildren.remove(activity.getContext());
                this.finishedChildren.remove(activity.getContext());
                this.checkFinishTransaction();
            } finally {
                this.legacyState.boundThreads.remove(Long.valueOf(Thread.currentThread().getId()));
                transactionHolder.remove();
            }

        }
    }

    private void checkFinishTransaction() {
        if (Agent.LOG.isFinestEnabled()) {
            this.threadAssertion();
        }

        if (this.runningChildren.isEmpty() && this.contextToTracer.isEmpty()) {
            this.finishTransaction();
        }

    }

    private final void threadAssertion() {
        if (Agent.LOG.isFinestEnabled() && !Thread.holdsLock(this.lock)) {
            Agent.LOG.log(Level.FINEST, "Thread assertion failed!",
                                 (new Exception("Thread assertion failed!")).fillInStackTrace());
        }

    }

    private String getTransactionName() {
        String fullName = this.getPriorityTransactionName().getName();
        String category = this.getPriorityTransactionName().getCategory();
        String prefix = this.getPriorityTransactionName().getPrefix();
        String txnNamePrefix = prefix + '/' + category + '/';
        return fullName != null && fullName.startsWith(txnNamePrefix) ? fullName.substring(txnNamePrefix.length(),
                                                                                                  fullName.length())
                       : fullName;
    }

    private static class LegacyState {
        final Set<Long> boundThreads;
        volatile Transaction rootTransaction;

        LegacyState() {
            MapMaker factory = (new MapMaker()).initialCapacity(8).concurrencyLevel(4);
            this.boundThreads = Sets.newSetFromMap(new LazyMapImpl(factory));
        }
    }

    protected class AppNameAndConfig {
        final PriorityApplicationName name;
        final AgentConfig config;

        AppNameAndConfig(PriorityApplicationName name, AgentConfig config) {
            this.name = name;
            this.config = config;
        }
    }
}
