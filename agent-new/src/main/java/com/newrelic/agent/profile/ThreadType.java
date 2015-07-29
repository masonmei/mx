package com.newrelic.agent.profile;

import com.newrelic.deps.org.json.simple.JSONAware;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import com.newrelic.deps.org.json.simple.JSONValue;
import java.io.IOException;
import java.io.Writer;

public abstract interface ThreadType extends JSONStreamAware, JSONAware
{
  public abstract String getName();

  public static enum BasicThreadType
    implements ThreadType
  {
    AGENT("agent"), 
    AGENT_INSTRUMENTATION("agent_instrumentation"), 
    REQUEST("request"), BACKGROUND("background"), OTHER("other");

    private String name;

    private BasicThreadType(String name) {
      this.name = name;
    }

    public String getName()
    {
      return this.name;
    }

    public void writeJSONString(Writer out) throws IOException
    {
      JSONValue.writeJSONString(this.name, out);
    }

    public String toJSONString()
    {
      return this.name;
    }
  }
}