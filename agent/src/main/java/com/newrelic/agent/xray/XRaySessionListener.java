package com.newrelic.agent.xray;

public abstract interface XRaySessionListener {
    public abstract void xraySessionCreated(XRaySession paramXRaySession);

    public abstract void xraySessionRemoved(XRaySession paramXRaySession);
}