package com.newrelic.agent;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import java.util.HashMap;

public class OutboundHeadersMap extends HashMap<String, String>
  implements OutboundHeaders
{
  private final HeaderType type;

  public OutboundHeadersMap(HeaderType type)
  {
    this.type = type;
  }

  public HeaderType getHeaderType()
  {
    return this.type;
  }

  public void setHeader(String name, String value)
  {
    put(name, value);
  }
}