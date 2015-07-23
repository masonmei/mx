package com.newrelic.agent.trace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.xray.XRaySession;

public class XRayTransactionSampler implements ITransactionSampler {
    static final int TRACES_TO_KEEP = 10;
    private final XRaySession session;
    private final String applicationName;
    private final List<TransactionData> data = new CopyOnWriteArrayList();

    public XRayTransactionSampler(XRaySession session) {
        this.session = session;
        applicationName = session.getApplicationName();
    }

    public boolean noticeTransaction(TransactionData td) {
        if (session.sessionHasExpired()) {
            return false;
        }
        if (data.size() >= 10) {
            return false;
        }
        String appName = td.getApplicationName();
        String transactionName = td.getBlameOrRootMetricName();
        if (isTransactionOfInterest(appName, transactionName)) {
            data.add(td);
            session.incrementCount();
            return true;
        }
        return false;
    }

    boolean isTransactionOfInterest(String appName, String transactionName) {
        if (!applicationName.equals(appName)) {
            return false;
        }
        if (!transactionName.equals(session.getKeyTransactionName())) {
            return false;
        }
        return true;
    }

    public List<TransactionTrace> harvest(String appName) {
        List tracesToReturn = new ArrayList();
        for (TransactionData td : data) {
            TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
            trace.setXraySessionId(session.getxRayId());
            tracesToReturn.add(trace);
        }
        data.clear();
        return tracesToReturn;
    }

    public void stop() {
    }

    public long getMaxDurationInNanos() {
        return 0L;
    }
}