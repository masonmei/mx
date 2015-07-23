package com.newrelic.agent.trace;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.IRPMService;
import com.newrelic.agent.IgnoreSilentlyException;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.xray.XRaySession;
import com.newrelic.agent.xray.XRaySessionListener;

public class TransactionTraceService extends AbstractService implements HarvestListener, TransactionListener,
                                                                                XRaySessionListener {
    private static final int INITIAL_TRACE_LIMIT = 5;
    private final ThreadMXBean threadMXBean;
    private final ConcurrentMap<String, ITransactionSampler> transactionTraceBuckets;
    private final boolean autoAppNameEnabled;
    private final boolean threadCpuTimeEnabled;
    private final ConfigService configService;
    private final ITransactionSampler requestTraceBucket;
    private final ITransactionSampler backgroundTraceBucket;
    private final SyntheticsTransactionSampler syntheticsTransactionSampler;
    private final List<ITransactionSampler> transactionSamplers = new CopyOnWriteArrayList();
    private final ConcurrentMap<Long, XRayTransactionSampler> xraySamplers;
    private final List<XRayTransactionSampler> xraySamplersPendingRemoval = new CopyOnWriteArrayList();

    public TransactionTraceService() {
        super(TransactionTraceService.class.getSimpleName());
        requestTraceBucket = createBucket();
        backgroundTraceBucket = createBucket();
        threadMXBean = ManagementFactory.getThreadMXBean();
        transactionTraceBuckets = new ConcurrentHashMap();
        configService = ServiceFactory.getConfigService();
        AgentConfig config = configService.getDefaultAgentConfig();
        autoAppNameEnabled = config.isAutoAppNamingEnabled();
        threadCpuTimeEnabled = initThreadCPUEnabled(config);
        syntheticsTransactionSampler = new SyntheticsTransactionSampler();
        xraySamplers = new ConcurrentHashMap();
    }

    public ThreadMXBean getThreadMXBean() {
        return threadMXBean;
    }

    public boolean isEnabled() {
        return true;
    }

    public void addTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionSamplers.add(transactionSampler);
    }

    public void removeTransactionTraceSampler(ITransactionSampler transactionSampler) {
        transactionSamplers.remove(transactionSampler);
    }

    public boolean isThreadCpuTimeEnabled() {
        return threadCpuTimeEnabled;
    }

    private boolean initThreadCPUEnabled(AgentConfig config) {
        boolean result = true;
        Boolean prop = (Boolean) config.getProperty("thread_cpu_time_enabled");
        if (prop == null) {
            String vendor = System.getProperty("java.vendor");

            if ("IBM Corporation".equals(vendor)) {
                return false;
            }
            return false;
        }
        result = prop.booleanValue();

        if (!result) {
            return false;
        }
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return (threadMXBean.isThreadCpuTimeSupported()) && (threadMXBean.isThreadCpuTimeEnabled());
    }

    private ITransactionSampler getTransactionTraceBucket(TransactionData transactionData) {
        if (autoAppNameEnabled) {
            return getOrCreateTransactionTraceBucket(transactionData.getApplicationName());
        }
        return getTransactionBucket(transactionData.getDispatcher());
    }

    private ITransactionSampler getOrCreateTransactionTraceBucket(String appName) {
        ITransactionSampler bucket = getTransactionTraceBucket(appName);
        if (bucket == null) {
            bucket = createBucket();
            ITransactionSampler oldBucket = (ITransactionSampler) transactionTraceBuckets.putIfAbsent(appName, bucket);
            if (oldBucket != null) {
                return oldBucket;
            }
        }
        return bucket;
    }

    private ITransactionSampler getTransactionTraceBucket(String bucketName) {
        if ("request" == bucketName) {
            return requestTraceBucket;
        }
        if ("background" == bucketName) {
            return backgroundTraceBucket;
        }
        return (ITransactionSampler) transactionTraceBuckets.get(bucketName);
    }

    private String getTransactionBucketName(Dispatcher dispatcher) {
        return dispatcher.isWebTransaction() ? "request" : "background";
    }

    private void noticeTransaction(TransactionData transactionData) {
        if (syntheticsTransactionSampler.noticeTransaction(transactionData)) {
            return;
        }
        for (ITransactionSampler transactionSampler : transactionSamplers) {
            if (transactionSampler.noticeTransaction(transactionData)) {
                return;
            }
        }
        ITransactionSampler bucket = getTransactionTraceBucket(transactionData);
        if (bucket != null) {
            bucket.noticeTransaction(transactionData);
        }
    }

    private ITransactionSampler getTransactionBucket(Dispatcher dispatcher) {
        ITransactionSampler bucket = getTransactionTraceBucket(getTransactionBucketName(dispatcher));
        if (bucket == null) {
            bucket = requestTraceBucket;
        }
        return bucket;
    }

    public void beforeHarvest(String appName, StatsEngine statsEngine) {
    }

    public void afterHarvest(String appName) {
        List traces = new ArrayList();
        if (autoAppNameEnabled) {
            traces.addAll(getExpensiveTransaction(appName));
        } else {
            traces.addAll(getAllExpensiveTransactions(appName));
        }
        traces.addAll(syntheticsTransactionSampler.harvest(appName));
        for (ITransactionSampler transactionSampler : transactionSamplers) {
            traces.addAll(transactionSampler.harvest(appName));
        }

        if (!traces.isEmpty()) {
            IRPMService rpmService = ServiceFactory.getRPMService(appName);
            sendTraces(rpmService, traces);
        }

        if (!xraySamplersPendingRemoval.isEmpty()) {
            for (XRayTransactionSampler samplerToRemove : xraySamplersPendingRemoval) {
                removeTransactionTraceSampler(samplerToRemove);
            }
            xraySamplersPendingRemoval.clear();
        }
    }

    private List<TransactionTrace> getAllExpensiveTransactions(String appName) {
        List traces = new ArrayList();
        List<ITransactionSampler> allBuckets =
                new ArrayList(Arrays.asList(new ITransactionSampler[] {requestTraceBucket, backgroundTraceBucket}));

        allBuckets.addAll(transactionTraceBuckets.values());
        for (ITransactionSampler bucket : allBuckets) {
            List expensiveTransactions = bucket.harvest(appName);
            if (expensiveTransactions != null) {
                traces.addAll(expensiveTransactions);
            }
        }
        return traces;
    }

    public List<TransactionTrace> getExpensiveTransaction(String appName) {
        ITransactionSampler bucket = getTransactionTraceBucket(appName);
        if (bucket != null) {
            return bucket.harvest(appName);
        }
        return Collections.emptyList();
    }

    private void sendTraces(IRPMService rpmService, List<TransactionTrace> traces) {
        if (!rpmService.isConnected()) {
            return;
        }
        try {
            rpmService.sendTransactionTraceData(traces);
        } catch (IgnoreSilentlyException e) {
        } catch (Exception e) {
            if (getLogger().isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Error sending transaction trace data to {0} for {1}: {2}",
                                                         new Object[] {rpmService.getHostString(),
                                                                              rpmService.getApplicationName(),
                                                                              e.getMessage()});

                if (getLogger().isLoggable(Level.FINEST)) {
                    getLogger().log(Level.FINEST, msg, e);
                } else {
                    getLogger().finer(msg);
                }
            }
        }
    }

    protected void doStart() {
        ServiceFactory.getTransactionService().addTransactionListener(this);
        ServiceFactory.getHarvestService().addHarvestListener(this);
        ServiceFactory.getXRaySessionService().addListener(this);
        RandomTransactionSampler.startSampler(INITIAL_TRACE_LIMIT);
    }

    protected void doStop() {
        for (ITransactionSampler bucket : transactionTraceBuckets.values()) {
            bucket.stop();
        }
        transactionTraceBuckets.clear();
    }

    private ITransactionSampler createBucket() {
        return new TransactionTraceBucket();
    }

    public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
        if (!transactionData.getTransactionTracerConfig().isEnabled()) {
            return;
        }
        noticeTransaction(transactionData);
    }

    public boolean isInteresting(Dispatcher dispatcher, long responseTimeNs) {
        return (autoAppNameEnabled) || (responseTimeNs > getTransactionBucket(dispatcher).getMaxDurationInNanos());
    }

    public void xraySessionCreated(XRaySession session) {
        getLogger().finer("TT service notified of X-Ray Session creation: " + session);
        XRayTransactionSampler sampler = new XRayTransactionSampler(session);
        xraySamplers.put(session.getxRayId(), sampler);
        transactionSamplers.add(0, sampler);
    }

    public void xraySessionRemoved(XRaySession session) {
        getLogger().finer("TT service notified of X-Ray Session removal: " + session);
        if (null != session) {
            XRayTransactionSampler samplerToRemove = (XRayTransactionSampler) xraySamplers.remove(session.getxRayId());

            if (samplerToRemove != null) {
                xraySamplersPendingRemoval.add(samplerToRemove);
            }
        }
    }
}