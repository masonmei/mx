package com.newrelic.agent;

import java.util.Map;

public abstract interface ConnectionListener {
    public abstract void connected(IRPMService paramIRPMService, Map<String, Object> paramMap);

    public abstract void disconnected(IRPMService paramIRPMService);
}