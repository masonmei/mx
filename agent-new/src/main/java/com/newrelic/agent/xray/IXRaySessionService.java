package com.newrelic.agent.xray;

import java.util.List;
import java.util.Map;

import com.newrelic.agent.IRPMService;
import com.newrelic.agent.service.Service;

public abstract interface IXRaySessionService extends Service {
    public abstract Map<?, ?> processSessionsList(List<Long> paramList, IRPMService paramIRPMService);

    public abstract boolean isEnabled();

    public abstract void addListener(XRaySessionListener paramXRaySessionListener);

    public abstract void removeListener(XRaySessionListener paramXRaySessionListener);
}