package com.newrelic.agent.service.module;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

import com.newrelic.deps.com.google.common.collect.ImmutableList;

public class Jar implements JSONStreamAware, Cloneable {
    private final String name;
    private final JarInfo jarInfo;

    public Jar(String name, JarInfo jarInfo) {
        this.name = name;
        this.jarInfo = jarInfo;
    }

    protected String getName() {
        return name;
    }

    protected String getVersion() {
        return jarInfo.version;
    }

    public void writeJSONString(Writer pWriter) throws IOException {
        List toSend = ImmutableList.of(name, jarInfo.version, jarInfo.attributes);

        JSONArray.writeJSONString(toSend, pWriter);
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (getVersion() == null ? 0 : getVersion().hashCode());
        result = 31 * result + (name == null ? 0 : name.hashCode());
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
        Jar other = (Jar) obj;
        if (getVersion() == null) {
            if (other.getVersion() != null) {
                return false;
            }
        } else if (!getVersion().equals(other.getVersion())) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }
}