package com.newrelic.api.agent;

import java.util.Map;

import com.newrelic.agent.bridge.AgentBridge;

public final class NewRelic {
    public static Agent getAgent() {
        return AgentBridge.agent;
    }

    public static void recordMetric(String name, float value) {
        getAgent().getMetricAggregator().recordMetric(name, value);
    }

    public static void recordResponseTimeMetric(String name, long millis) {
        getAgent().getMetricAggregator().recordResponseTimeMetric(name, millis);
    }

    public static void incrementCounter(String name) {
        getAgent().getMetricAggregator().incrementCounter(name);
    }

    public static void incrementCounter(String name, int count) {
        getAgent().getMetricAggregator().incrementCounter(name, count);
    }

    public static void noticeError(Throwable throwable, Map<String, String> params) {
        AgentBridge.publicApi.noticeError(throwable, params);
    }

    public static void noticeError(Throwable throwable) {
        AgentBridge.publicApi.noticeError(throwable);
    }

    public static void noticeError(String message, Map<String, String> params) {
        AgentBridge.publicApi.noticeError(message, params);
    }

    public static void noticeError(String message) {
        AgentBridge.publicApi.noticeError(message);
    }

    public static void addCustomParameter(String key, Number value) {
        AgentBridge.publicApi.addCustomParameter(key, value);
    }

    public static void addCustomParameter(String key, String value) {
        AgentBridge.publicApi.addCustomParameter(key, value);
    }

    public static void setTransactionName(String category, String name) {
        AgentBridge.publicApi.setTransactionName(category, name);
    }

    public static void ignoreTransaction() {
        AgentBridge.publicApi.ignoreTransaction();
    }

    public static void ignoreApdex() {
        AgentBridge.publicApi.ignoreApdex();
    }

    public static void setRequestAndResponse(Request request, Response response) {
        AgentBridge.publicApi.setRequestAndResponse(request, response);
    }

    public static String getBrowserTimingHeader() {
        return AgentBridge.publicApi.getBrowserTimingHeader();
    }

    public static String getBrowserTimingFooter() {
        return AgentBridge.publicApi.getBrowserTimingFooter();
    }

    public static void setUserName(String name) {
        AgentBridge.publicApi.setUserName(name);
    }

    public static void setAccountName(String name) {
        AgentBridge.publicApi.setAccountName(name);
    }

    public static void setProductName(String name) {
        AgentBridge.publicApi.setProductName(name);
    }
}