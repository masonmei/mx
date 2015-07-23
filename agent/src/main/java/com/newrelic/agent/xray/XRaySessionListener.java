package com.newrelic.agent.xray;

public interface XRaySessionListener {
    void xraySessionCreated(XRaySession paramXRaySession);

    void xraySessionRemoved(XRaySession paramXRaySession);
}