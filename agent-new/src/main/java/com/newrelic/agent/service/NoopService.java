package com.newrelic.agent.service;

public class NoopService extends AbstractService {
    public NoopService(String serviceName) {
        super(serviceName);
    }

    public final boolean isEnabled() {
        return false;
    }

    protected final void doStart() throws Exception {
    }

    protected final void doStop() throws Exception {
    }
}