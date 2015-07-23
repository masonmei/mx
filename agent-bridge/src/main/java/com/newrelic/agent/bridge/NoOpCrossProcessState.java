package com.newrelic.agent.bridge;

import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

public class NoOpCrossProcessState implements CrossProcessState {
    public static final CrossProcessState INSTANCE = new NoOpCrossProcessState();

    public void processOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
    }

    public void processOutboundResponseHeaders(OutboundHeaders outboundHeaders, long contentLength) {
    }

    public String getRequestMetadata() {
        return null;
    }

    public void processRequestMetadata(String requestMetadata) {
    }

    public String getResponseMetadata() {
        return null;
    }

    public void processResponseMetadata(String responseMetadata) {
    }

    public void processInboundResponseHeaders(InboundHeaders inboundHeaders, TracedMethod tracer, String host,
                                              String uri, boolean addRollupMetric) {
    }
}