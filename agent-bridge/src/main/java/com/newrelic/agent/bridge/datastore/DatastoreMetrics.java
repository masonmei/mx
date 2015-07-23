package com.newrelic.agent.bridge.datastore;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;

public class DatastoreMetrics {
    public static final String METRIC_NAMESPACE = "Datastore";
    public static final String ALL = "Datastore/all";
    public static final String ALL_WEB = "Datastore/allWeb";
    public static final String ALL_OTHER = "Datastore/allOther";
    public static final String VENDOR_ALL = "Datastore/{0}/all";
    public static final String VENDOR_ALL_WEB = "Datastore/{0}/allWeb";
    public static final String VENDOR_ALL_OTHER = "Datastore/{0}/allOther";
    public static final String STATEMENT_METRIC = "Datastore/statement/{0}/{1}/{2}";
    public static final String OPERATION_METRIC = "Datastore/operation/{0}/{1}";
    public static final String INSTANCE_METRIC = "Datastore/instance/{0}/{1}:{2}/{3}";
    public static final String DEFAULT_OPERATION = "other";
    public static final String DEFAULT_TABLE = "other";
    private static Map<DatastoreVendor, DatastoreMetrics> instances = new HashMap(DatastoreVendor.values().length);
    private final DatastoreVendor datastoreVendor;

    private DatastoreMetrics(DatastoreVendor dbVendor) {
        datastoreVendor = dbVendor;
    }

    public static DatastoreMetrics getInstance(DatastoreVendor datastoreVendor) {
        DatastoreMetrics instance = (DatastoreMetrics) instances.get(datastoreVendor);
        if (null == instance) {
            synchronized(instances) {
                instance = (DatastoreMetrics) instances.get(datastoreVendor);
                if (null == instance) {
                    instance = new DatastoreMetrics(datastoreVendor);
                    instances.put(datastoreVendor, instance);
                }
            }
        }
        return instance;
    }

    public static Set<DatastoreVendor> getInstanceNames() {
        return instances.keySet();
    }

    public void collectDatastoreMetrics(Transaction tx, TracedMethod method, String table, String operation,
                                        String host, String port) {
        if (null == table) {
            method.setMetricName(new String[] {MessageFormat.format("Datastore/operation/{0}/{1}",
                                                                           new Object[] {datastoreVendor, operation})});
        } else {
            method.addRollupMetricName(new String[] {MessageFormat.format("Datastore/operation/{0}/{1}",
                                                                                 new Object[] {datastoreVendor,
                                                                                                      operation})});
            method.setMetricName(new String[] {MessageFormat.format("Datastore/statement/{0}/{1}/{2}",
                                                                           new Object[] {datastoreVendor, table,
                                                                                                operation})});
        }

        method.addRollupMetricName(new String[] {"Datastore/all"});
        method.addRollupMetricName(new String[] {MessageFormat
                                                         .format("Datastore/{0}/all", new Object[] {datastoreVendor})});
        if (tx.isWebTransaction()) {
            method.addRollupMetricName(new String[] {"Datastore/allWeb"});
            method.addRollupMetricName(new String[] {MessageFormat.format("Datastore/{0}/allWeb",
                                                                                 new Object[] {datastoreVendor})});
        } else {
            method.addRollupMetricName(new String[] {"Datastore/allOther"});
            method.addRollupMetricName(new String[] {MessageFormat.format("Datastore/{0}/allOther",
                                                                                 new Object[] {datastoreVendor})});
        }
    }

    public void unparsedQuerySupportability() {
        NewRelic.incrementCounter(MessageFormat.format("Supportability/Datastore/{0}/unparsedQuery",
                                                              new Object[] {datastoreVendor}));
    }
}