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
        this.applicationName = session.getApplicationName();
    }

    public boolean noticeTransaction(TransactionData td) {
        if (this.session.sessionHasExpired()) {
            return false;
        }
        if (this.data.size() >= 10) {
            return false;
        }
        String appName = td.getApplicationName();
        String transactionName = td.getBlameOrRootMetricName();
        if (isTransactionOfInterest(appName, transactionName)) {
            this.data.add(td);
            this.session.incrementCount();
            return true;
        }
        return false;
    }

    boolean isTransactionOfInterest(String appName, String transactionName) {
        if (!this.applicationName.equals(appName)) {
            return false;
        }
        if (!transactionName.equals(this.session.getKeyTransactionName())) {
            return false;
        }
        return true;
    }

    public List<TransactionTrace> harvest(String appName) {
        List tracesToReturn = new ArrayList();
        for (TransactionData td : this.data) {
            TransactionTrace trace = TransactionTrace.getTransactionTrace(td);
            trace.setXraySessionId(this.session.getxRayId());
            tracesToReturn.add(trace);
        }
        this.data.clear();
        return tracesToReturn;
    }

    public void stop() {
    }

    public long getMaxDurationInNanos() {
        return 0L;
    }
}