package com.newrelic.agent.jmx.create;

import javax.management.MalformedObjectNameException;

public class JmxInvoke extends JmxObject
{
  private final String operationName;
  private final Object[] params;
  private final String[] signature;
  private int errorCount = 0;

  public JmxInvoke(String pObjectName, String safeName, String pOperationName, Object[] pParams, String[] pSignature)
    throws MalformedObjectNameException
  {
    super(pObjectName, safeName);
    this.operationName = pOperationName;
    this.params = pParams;
    this.signature = pSignature;
  }

  public String getOperationName()
  {
    return this.operationName;
  }

  public Object[] getParams()
  {
    return this.params;
  }

  public String[] getSignature()
  {
    return this.signature;
  }

  public int getErrorCount()
  {
    return this.errorCount;
  }

  public void incrementErrorCount()
  {
    this.errorCount += 1;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("object_name: ").append(getObjectNameString());
    sb.append(" operation_name: ").append(this.operationName);
    sb.append(" error_count: ").append(this.errorCount);
    return sb.toString();
  }
}