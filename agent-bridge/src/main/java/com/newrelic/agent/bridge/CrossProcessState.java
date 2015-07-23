package com.newrelic.agent.bridge;

import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

public interface CrossProcessState {
    void processOutboundRequestHeaders(OutboundHeaders paramOutboundHeaders);

    void processOutboundResponseHeaders(OutboundHeaders paramOutboundHeaders, long paramLong);

    void processInboundResponseHeaders(InboundHeaders paramInboundHeaders, TracedMethod paramTracedMethod,
                                       String paramString1, String paramString2, boolean paramBoolean);

    String getRequestMetadata();

    void processRequestMetadata(String paramString);

    String getResponseMetadata();

    void processResponseMetadata(String paramString);
}