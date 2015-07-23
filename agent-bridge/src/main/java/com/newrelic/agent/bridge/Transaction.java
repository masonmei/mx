package com.newrelic.agent.bridge;

import java.util.Map;

import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public interface Transaction extends com.newrelic.api.agent.Transaction {
    Map<String, Object> getAgentAttributes();

    boolean setTransactionName(TransactionNamePriority paramTransactionNamePriority, boolean paramBoolean,
                               String paramString, String[] paramArrayOfString);

    void beforeSendResponseHeaders();

    boolean isStarted();

    void setApplicationName(ApplicationNamePriority paramApplicationNamePriority, String paramString);

    boolean isAutoAppNamingEnabled();

    boolean isWebRequestSet();

    boolean isWebResponseSet();

    void setWebRequest(Request paramRequest);

    void provideHeaders(InboundHeaders paramInboundHeaders);

    WebResponse getWebResponse();

    void setWebResponse(Response paramResponse);

    void convertToWebTransaction();

    boolean isWebTransaction();

    void requestInitialized(Request paramRequest, Response paramResponse);

    void requestDestroyed();

    void saveMessageParameters(Map<String, String> paramMap);

    CrossProcessState getCrossProcessState();

    TracedMethod startFlyweightTracer();

    void finishFlyweightTracer(TracedMethod paramTracedMethod, long paramLong1, long paramLong2, String paramString1,
                               String paramString2, String paramString3, String paramString4,
                               String[] paramArrayOfString);

    boolean registerAsyncActivity(Object paramObject);

    boolean startAsyncActivity(Object paramObject);

    boolean ignoreAsyncActivity(Object paramObject);
}