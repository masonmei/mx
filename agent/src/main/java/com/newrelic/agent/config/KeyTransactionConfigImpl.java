package com.newrelic.agent.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

final class KeyTransactionConfigImpl implements KeyTransactionConfig {
    private final Map<String, Long> apdexTs;
    private final long apdexTInMillis;

    private KeyTransactionConfigImpl(Map<String, Object> props, long apdexTInMillis) {
        this.apdexTInMillis = apdexTInMillis;
        Map apdexTs = new HashMap();
        for (Entry entry : props.entrySet()) {
            Object apdexT = entry.getValue();
            if ((apdexT instanceof Number)) {
                Long apdexTinMillis = Long.valueOf((long) (((Number) apdexT).doubleValue() * 1000.0D));
                String txName = (String) entry.getKey();
                apdexTs.put(txName, apdexTinMillis);
            }
        }
        this.apdexTs = Collections.unmodifiableMap(apdexTs);
    }

    static KeyTransactionConfig createKeyTransactionConfig(Map<String, Object> settings, long apdexTInMillis) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new KeyTransactionConfigImpl(settings, apdexTInMillis);
    }

    public boolean isApdexTSet(String transactionName) {
        return apdexTs.containsKey(transactionName);
    }

    public long getApdexTInMillis(String transactionName) {
        Long apdexT = (Long) apdexTs.get(transactionName);
        if (apdexT == null) {
            return apdexTInMillis;
        }
        return apdexT.longValue();
    }
}