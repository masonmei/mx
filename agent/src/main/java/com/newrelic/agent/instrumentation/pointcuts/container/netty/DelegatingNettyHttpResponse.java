package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class DelegatingNettyHttpResponse implements Response {
    private volatile NettyHttpResponse delegate;

    private DelegatingNettyHttpResponse(NettyHttpResponse delegate) {
        this.delegate = delegate;
    }

    static Response create(NettyHttpResponse delegate) {
        return new DelegatingNettyHttpResponse(delegate);
    }

    public void setDelegate(NettyHttpResponse delegate) {
        this.delegate = delegate;
    }

    public HeaderType getHeaderType() {
        return HeaderType.HTTP;
    }

    public int getStatus() throws Exception {
        return delegate == null ? 0 : delegate._nr_status().getCode();
    }

    public String getStatusMessage() throws Exception {
        return delegate == null ? "" : delegate._nr_status().getReasonPhrase();
    }

    public String getContentType() {
        return null;
    }

    public void setHeader(String name, String value) {
        if (delegate == null) {
            return;
        }
        delegate.setHeader(name, value);
    }
}