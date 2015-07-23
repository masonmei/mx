package com.newrelic.agent;

import java.io.UnsupportedEncodingException;

import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;

public class DeobfuscatedInboundHeaders implements InboundHeaders {
    InboundHeaders delegate;
    String encodingKey;

    public DeobfuscatedInboundHeaders(InboundHeaders headers, String encodingKey) {
        delegate = headers;
        this.encodingKey = encodingKey;
    }

    public HeaderType getHeaderType() {
        return delegate.getHeaderType();
    }

    public String getHeader(String name) {
        if (encodingKey == null) {
            return null;
        }

        if (HeadersUtil.NEWRELIC_HEADERS.contains(name)) {
            String obfuscatedValue = delegate.getHeader(name);
            if (obfuscatedValue == null) {
                return null;
            }
            try {
                return Obfuscator.deobfuscateNameUsingKey(obfuscatedValue, encodingKey);
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }

        return delegate.getHeader(name);
    }
}