package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;

public abstract interface RUMState
{
  public abstract RUMState process(Transaction paramTransaction, GenerateVisitor paramGenerateVisitor,
                                   TemplateText paramTemplateText, String paramString)
    throws Exception;
}