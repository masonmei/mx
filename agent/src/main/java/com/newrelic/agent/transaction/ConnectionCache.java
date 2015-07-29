package com.newrelic.agent.transaction;

import java.sql.Connection;

import com.newrelic.agent.instrumentation.pointcuts.database.ConnectionFactory;
import com.newrelic.deps.com.google.common.cache.Cache;
import com.newrelic.deps.com.google.common.cache.CacheBuilder;

public class ConnectionCache {
    private static final int MAX_CONN_CACHE_SIZE = 50;
    private Cache<Connection, ConnectionFactory> connectionFactoryCache;

    public void putConnectionFactory(Connection key, ConnectionFactory val) {
        getOrCreateConnectionFactoryCache().put(key, val);
    }

    public long getConnectionFactoryCacheSize() {
        return getOrCreateConnectionFactoryCache().size();
    }

    public ConnectionFactory removeConnectionFactory(Connection key) {
        if (connectionFactoryCache == null) {
            return null;
        }
        ConnectionFactory cf = (ConnectionFactory) connectionFactoryCache.getIfPresent(key);
        connectionFactoryCache.invalidate(key);
        return cf;
    }

    public Cache<Connection, ConnectionFactory> getConnectionFactoryCache() {
        return connectionFactoryCache;
    }

    private Cache<Connection, ConnectionFactory> getOrCreateConnectionFactoryCache() {
        if (connectionFactoryCache == null) {
            connectionFactoryCache = CacheBuilder.newBuilder().maximumSize(50L).build();
        }
        return connectionFactoryCache;
    }

    public void clear() {
        connectionFactoryCache.invalidateAll();
    }
}