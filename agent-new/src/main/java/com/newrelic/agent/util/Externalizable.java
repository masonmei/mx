package com.newrelic.agent.util;

import java.io.DataOutputStream;
import java.io.IOException;

public abstract interface Externalizable
{
  public abstract void write(DataOutputStream paramDataOutputStream)
    throws IOException;
}