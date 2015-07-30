package com.newrelic.agent.application;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import com.newrelic.agent.config.BaseConfig;
import com.newrelic.api.agent.ApplicationNamePriority;

public class PriorityApplicationName {
    public static final PriorityApplicationName NONE = create(null, ApplicationNamePriority.NONE);
    private final ApplicationNamePriority priority;
    private final String name;
    private final List<String> names;

    private PriorityApplicationName(String name, ApplicationNamePriority priority) {
        this.priority = priority;
        if (name == null) {
            this.name = null;
            names = null;
        } else {
            names = Collections.unmodifiableList(BaseConfig.getUniqueStringsFromString(name, ";"));
            this.name = names.get(0);
        }
    }

    public static PriorityApplicationName create(String name, ApplicationNamePriority priority) {
        return new PriorityApplicationName(name, priority);
    }

    public String getName() {
        return name;
    }

    public List<String> getNames() {
        return names;
    }

    public ApplicationNamePriority getPriority() {
        return priority;
    }

    public String toString() {
        return MessageFormat.format("{0}[name={1}, priority={2}]", getClass().getName(), getName(), getPriority());
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (this.name == null ? 0 : this.name.hashCode());
        result = prime * result + (this.priority == null ? 0 : this.priority.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PriorityApplicationName other = (PriorityApplicationName) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        return priority == other.priority;
    }
}