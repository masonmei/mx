package com.newrelic.agent.metric;

import com.newrelic.deps.org.json.simple.JSONObject;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public final class MetricName
  implements JSONStreamAware
{
  public static final MetricName WEB_TRANSACTION_ORM_ALL = create("ORM/allWeb");

  public static final MetricName OTHER_TRANSACTION_ORM_ALL = create("ORM/allOther");

  public static final MetricName WEB_TRANSACTION_SOLR_ALL = create("Solr/allWeb");

  public static final MetricName OTHER_TRANSACTION_SOLR_ALL = create("Solr/allOther");

  public static final MetricName QUEUE_TIME = create("WebFrontend/QueueTime");
  public static final MetricName WEBFRONTEND_WEBSERVER_ALL = create("WebFrontend/WebServer/all");
  private static final String NAME_KEY = "name";
  private static final String SCOPE_KEY = "scope";
  public static final String EMPTY_SCOPE = "";
  private final String name;
  private final String scope;
  private final int hashCode;

  private MetricName(String name, String scope)
  {
    this.name = name;
    this.scope = scope;
    this.hashCode = generateHashCode(name, scope);
  }

  public String getName() {
    return this.name;
  }

  public String getScope() {
    return this.scope;
  }

  public boolean isScoped() {
    return this.scope != "";
  }

  public int hashCode()
  {
    return this.hashCode;
  }

  public static int generateHashCode(String name, String scope) {
    int prime = 31;
    int result = 1;
    result = 31 * result + name.hashCode();
    result = 31 * result + scope.hashCode();
    return result;
  }

  public boolean equals(Object obj)
  {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MetricName other = (MetricName)obj;
    return (this.name.equals(other.name)) && (this.scope.equals(other.scope));
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder(this.name);
    if (isScoped()) {
      sb.append(" (").append(this.scope).append(')');
    }
    return sb.toString();
  }

  public void writeJSONString(Writer writer)
    throws IOException
  {
    Map props = new HashMap(3);
    if (isScoped()) {
      props.put("scope", this.scope);
    }
    props.put("name", this.name);
    JSONObject.writeJSONString(props, writer);
  }

  public static MetricName create(String name, String scope) {
    if ((name == null) || (name.length() == 0)) {
      return null;
    }
    if ((scope == null) || (scope.length() == 0)) {
      scope = "";
    }
    return new MetricName(name, scope);
  }

  public static MetricName create(String name) {
    return create(name, null);
  }

  public static MetricName parseJSON(JSONObject jsonObj) {
    String scope = (String)String.class.cast(jsonObj.get("scope"));
    String name = (String)String.class.cast(jsonObj.get("name"));
    return create(name, scope);
  }
}