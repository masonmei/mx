package com.newrelic.agent.instrumentation.pointcuts.frameworks.struts;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(originalClassName = {"com/opensymphony/xwork2/ActionProxy"})
public abstract interface ActionProxy {
    public abstract String getActionName();
}