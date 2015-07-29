package com.newrelic.agent.attributes;

import com.newrelic.agent.Transaction;
import java.util.Map;

public class AgentAttributeSender extends AttributeSender
{
  protected static String ATTRIBUTE_TYPE = "agent";

  protected String getAttributeType()
  {
    return ATTRIBUTE_TYPE;
  }

  protected Map<String, Object> getAttributeMap() throws Throwable
  {
    Transaction tx = Transaction.getTransaction();
    return tx.getAgentAttributes();
  }
}