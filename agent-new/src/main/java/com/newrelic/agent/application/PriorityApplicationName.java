package com.newrelic.agent.application;

import com.newrelic.agent.config.BaseConfig;
import com.newrelic.api.agent.ApplicationNamePriority;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

public class PriorityApplicationName
{
  public static final PriorityApplicationName NONE = create(null, ApplicationNamePriority.NONE);
  private final ApplicationNamePriority priority;
  private final String name;
  private final List<String> names;

  private PriorityApplicationName(String name, ApplicationNamePriority priority)
  {
    this.priority = priority;
    if (name == null) {
      this.name = null;
      this.names = null;
    } else {
      this.names = Collections.unmodifiableList(BaseConfig.getUniqueStringsFromString(name, ";"));

      this.name = ((String)this.names.get(0));
    }
  }

  public String getName() {
    return this.name;
  }

  public List<String> getNames() {
    return this.names;
  }

  public ApplicationNamePriority getPriority() {
    return this.priority;
  }

  public String toString()
  {
    return MessageFormat.format("{0}[name={1}, priority={2}]", new Object[] { getClass().getName(), getName(), getPriority() });
  }

  public static PriorityApplicationName create(String name, ApplicationNamePriority priority) {
    return new PriorityApplicationName(name, priority);
  }

  public int hashCode()
  {
    int prime = 31;
    int result = 1;
    result = 31 * result + (this.name == null ? 0 : this.name.hashCode());
    result = 31 * result + (this.priority == null ? 0 : this.priority.hashCode());
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
    PriorityApplicationName other = (PriorityApplicationName)obj;
    if (this.name == null) {
      if (other.name != null)
        return false;
    }
    else if (!this.name.equals(other.name)) {
      return false;
    }
    if (this.priority != other.priority) {
      return false;
    }
    return true;
  }
}