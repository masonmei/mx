package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;

public class DoneState extends AbstractRUMState {
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
            throws Exception {
        writeText(tx, generator, node, text);
        return this;
    }
}