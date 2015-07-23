package com.newrelic.agent.transaction;

import java.net.URL;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.newrelic.agent.tracers.MetricNameFormatWithHost;

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
        if (inputStreamCache == null) {
            inputStreamCache = CacheBuilder.newBuilder().weakKeys().build();
        }
        return inputStreamCache;
    }

    public Object removeSolrResponseBuilderParamName() {
        Object toReturn = solrResponseBuilder;
        solrResponseBuilder = null;
        return toReturn;
    }

    public void putSolrResponseBuilderParamName(Object val) {
        solrResponseBuilder = val;
    }

    public URL getURL(Object key) {
        return (URL) getUrlCache().getIfPresent(key);
    }

    public void putURL(Object key, URL val) {
        getUrlCache().put(key, val);
    }

    private Cache<Object, URL> getUrlCache() {
        if (urlCache == null) {
            urlCache = CacheBuilder.newBuilder().weakKeys().build();
        }
        return urlCache;
    }
}