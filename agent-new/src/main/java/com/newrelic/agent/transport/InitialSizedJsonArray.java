package com.newrelic.agent.transport;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class InitialSizedJsonArray
  implements JSONStreamAware
{
  private List<Object> toSend;

  public InitialSizedJsonArray(int size)
  {
    if (size > 0)
      this.toSend = new ArrayList(size);
    else
      this.toSend = Collections.emptyList();
  }

  public void add(Object obj)
  {
    this.toSend.add(obj);
  }

  public void addAll(Collection<Object> objs) {
    this.toSend.addAll(objs);
  }

  public int size() {
    return this.toSend.size();
  }

  public void writeJSONString(Writer out) throws IOException
  {
    JSONArray.writeJSONString(this.toSend, out);
  }
}