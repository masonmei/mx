package com.newrelic.agent.trace;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TransactionData;

public class SyntheticsTransactionSampler implements ITransactionSampler {
    static final int MAX_SYNTHETIC_TRANSACTION_PER_HARVEST = 20;
    private static final TransactionData queueMarker = new TransactionData(null, 0);
    private final ConcurrentLinkedQueue<TransactionData> pending = new ConcurrentLinkedQueue();
    private final AtomicInteger pendingCount = new AtomicInteger(0);

    public boolean noticeTransaction(TransactionData td) {
        if (td.isSyntheticTransaction()) {
            if (pendingCount.get() < MAX_SYNTHETIC_TRANSACTION_PER_HARVEST) {
                pendingCount.incrementAndGet();
                pending.add(td);
                String msg = MessageFormat.format("Sampled Synthetics Transaction: {0}", new Object[] {td});
                Agent.LOG.finest(msg);
                return true;
            }
            Agent.LOG.log(Level.FINER, "Dropped Synthetic TT for app {0}", new Object[] {td.getApplicationName()});
        }

        return false;
    }

    public List<TransactionTrace> harvest(String appName) {
        List result = new LinkedList();
        if (appName == null) {
            return result;
        }

        pending.add(queueMarker);
        int removedCount = 0;
        TransactionData queued;
        while ((queued = (TransactionData) pending.poll()) != queueMarker) {
            if (appName.equals(queued.getApplicationName())) {
                TransactionTrace tt = TransactionTrace.getTransactionTrace(queued);
                tt.setSyntheticsResourceId(queued.getSyntheticsResourceId());
                removedCount++;
                result.add(tt);
            } else {
                pending.add(queued);
            }
        }
        pendingCount.addAndGet(-removedCount);
        return result;
    }

    public void stop() {
    }

    public long getMaxDurationInNanos() {
        return 1L;
    }

    int getPendingCount() {
        return pendingCount.get();
    }
}