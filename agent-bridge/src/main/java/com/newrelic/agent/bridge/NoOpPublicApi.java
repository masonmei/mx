package com.newrelic.agent.bridge;

import java.util.Map;

import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

class NoOpPublicApi implements PublicApi {
    public void noticeError(Throwable throwable, Map<String, String> params) {
    }

    public void noticeError(Throwable throwable) {
    }

    public void noticeError(String message, Map<String, String> params) {
    }

    public void noticeError(String message) {
    }

    public void addCustomParameter(String key, Number value) {
    }

    public void addCustomParameter(String key, String value) {
    }

    public void setTransactionName(String category, String name) {
    }

    public void ignoreTransaction() {
    }

    public void ignoreApdex() {
    }

    public void setRequestAndResponse(Request request, Response response) {
    }

    public String getBrowserTimingHeader() {
        return "";
    }

    public String getBrowserTimingFooter() {
        return "";
    }

    public void setUserName(String name) {
    }

    public void setAccountName(String name) {
    }

    public void setProductName(String name) {
    }
}