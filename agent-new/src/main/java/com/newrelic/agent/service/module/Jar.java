package com.newrelic.agent.service.module;

import com.newrelic.deps.com.google.common.collect.ImmutableList;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class Jar
  implements JSONStreamAware, Cloneable
{
  private final String name;
  private final JarInfo jarInfo;

  public Jar(String name, JarInfo jarInfo)
  {
    this.name = name;
    this.jarInfo = jarInfo;
  }

  protected String getName()
  {
    return this.name;
  }

  protected String getVersion()
  {
    return this.jarInfo.version;
  }

  public void writeJSONString(Writer pWriter)
    throws IOException
  {
    List toSend = ImmutableList.of(this.name, this.jarInfo.version, this.jarInfo.attributes);

    JSONArray.writeJSONString(toSend, pWriter);
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (getVersion() == null ? 0 : getVersion().hashCode());
    result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
    return result;
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Jar other = (Jar)obj;
    if (getVersion() == null) {
      if (other.getVersion() != null)
        return false;
    } else if (!getVersion().equals(other.getVersion()))
      return false;
    if (this.name == null) {
      if (other.name != null)
        return false;
    } else if (!this.name.equals(other.name))
      return false;
    return true;
  }
}