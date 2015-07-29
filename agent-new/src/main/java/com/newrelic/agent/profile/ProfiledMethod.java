package com.newrelic.agent.profile;

import com.newrelic.agent.Agent;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.profile.method.MethodInfo;
import com.newrelic.agent.profile.method.MethodInfoUtil;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;

public class ProfiledMethod
  implements JSONStreamAware
{
  private final StackTraceElement stackTraceElement;
  private final int hashCode;
  private MethodInfo info;

  private ProfiledMethod(StackTraceElement stackTraceElement)
  {
    this.stackTraceElement = stackTraceElement;
    this.hashCode = stackTraceElement.hashCode();
  }

  public static ProfiledMethod newProfiledMethod(StackTraceElement stackElement)
  {
    if (stackElement == null) {
      return null;
    }
    if (stackElement.getClassName() == null) {
      return null;
    }
    if (stackElement.getMethodName() == null) {
      return null;
    }

    return new ProfiledMethod(stackElement);
  }

  public String getFullMethodName() {
    return getClassName() + ":" + getMethodName();
  }

  public String getMethodName() {
    return this.stackTraceElement.getMethodName();
  }

  public String getClassName() {
    return this.stackTraceElement.getClassName();
  }

  public final int getLineNumber() {
    return this.stackTraceElement.getLineNumber();
  }

  public int hashCode()
  {
    return this.hashCode;
  }

  public boolean equals(Object obj)
  {
    if (obj == this) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ProfiledMethod other = (ProfiledMethod)obj;
    return other.stackTraceElement.equals(this.stackTraceElement);
  }

  public String toString()
  {
    return getFullMethodName() + ":" + getLineNumber();
  }

  public void writeJSONString(Writer out) throws IOException
  {
    if (this.info == null)
      JSONArray.writeJSONString(Arrays.asList(new Serializable[] { getClassName(), getMethodName(), Integer.valueOf(getLineNumber()) }), out);
    else
      JSONArray.writeJSONString(Arrays.asList(new Object[] { getClassName(), getMethodName(), Integer.valueOf(getLineNumber()), this.info.getJsonMethodMaps() }), out);
  }

  void setMethodDetails(Map<String, Class<?>> classMap)
  {
    Class declaringClass = (Class)classMap.get(getClassName());
    if (declaringClass != null)
      try {
        this.info = MethodInfoUtil.createMethodInfo(declaringClass, getMethodName(), getLineNumber());
      } catch (Throwable e) {
        Agent.LOG.log(Level.FINER, e, "Error finding MethodInfo for {0}.{1}", new Object[] { declaringClass.getName(), getMethodName() });
        this.info = null;
      }
  }
}