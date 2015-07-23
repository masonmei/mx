package com.newrelic.agent.tracers.jasper;

public abstract interface GenerateVisitor extends Visitor {
    public abstract void visit(TemplateText paramTemplateText) throws Exception;
}