package com.newrelic.agent.transaction;

import java.net.URL;

import com.newrelic.agent.tracers.MetricNameFormatWithHost;
import com.newrelic.deps.com.google.common.cache.Cache;
import com.newrelic.deps.com.google.common.cache.CacheBuilder;

public class TransactionCache {
    private Cache<Object, URL> urlCache;
    private Cache<Object, MetricNameFormatWithHost> inputStreamCache;
    private Object solrResponseBuilder;

    public MetricNameFormatWithHost getMetricNameFormatWithHost(Object key) {
        return (MetricNameFormatWithHost) getInputStreamCache().getIfPresent(key);
    }

    public void putMetricNameFormatWithHost(Object key, MetricNameFormatWithHost val) {
        getInputStreamCache().put(key, val);
    }

    private Cache<Object, MetricNameFormatWithHost> getInputStreamCache() {
        if (this.inputStreamCache == null) {
            this.inputStreamCache = CacheBuilder.newBuilder().weakKeys().build();
        }
        return this.inputStreamCache;
    }

    public Object removeSolrResponseBuilderParamName() {
        Object toReturn = this.solrResponseBuilder;
        this.solrResponseBuilder = null;
        return toReturn;
    }

    public void putSolrResponseBuilderParamName(Object val) {
        this.solrResponseBuilder = val;
    }

    public URL getURL(Object key) {
        return (URL) getUrlCache().getIfPresent(key);
    }

    public void putURL(Object key, URL val) {
        getUrlCache().put(key, val);
    }

    private Cache<Object, URL> getUrlCache() {
        if (this.urlCache == null) {
            this.urlCache = CacheBuilder.newBuilder().weakKeys().build();
        }
        return this.urlCache;
    }
}