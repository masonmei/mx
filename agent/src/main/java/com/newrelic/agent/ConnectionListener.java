package com.newrelic.agent;

import java.util.Map;

public interface ConnectionListener {
    void connected(IRPMService paramIRPMService, Map<String, Object> paramMap);

    void disconnected(IRPMService paramIRPMService);
}