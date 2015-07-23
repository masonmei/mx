package com.newrelic.agent.bridge;

import java.util.Map;

import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public interface PublicApi {
    void noticeError(Throwable paramThrowable, Map<String, String> paramMap);

    void noticeError(Throwable paramThrowable);

    void noticeError(String paramString, Map<String, String> paramMap);

    void noticeError(String paramString);

    void addCustomParameter(String paramString, Number paramNumber);

    void addCustomParameter(String paramString1, String paramString2);

    void setTransactionName(String paramString1, String paramString2);

    void ignoreTransaction();

    void ignoreApdex();

    void setRequestAndResponse(Request paramRequest, Response paramResponse);

    String getBrowserTimingHeader();

    String getBrowserTimingFooter();

    void setUserName(String paramString);

    void setAccountName(String paramString);

    void setProductName(String paramString);
}