//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.service.module;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

class JarInfo {
    static final JarInfo MISSING = new JarInfo(" ", ImmutableMap.<String, String>of());
    public final String version;
    public final Map<String, String> attributes;

    public JarInfo(String version, Map<String, String> attributes) {
        this.version = version == null ? " " : version;
        this.attributes = attributes == null ? ImmutableMap.<String, String>of() : ImmutableMap.copyOf(attributes);
    }

    public String toString() {
        return "JarInfo [version=" + this.version + ", attributes=" + this.attributes + "]";
    }

    public int hashCode() {
        boolean prime = true;
        byte result = 1;
        int result1 = 31 * result + (this.attributes == null ? 0 : this.attributes.hashCode());
        result1 = 31 * result1 + (this.version == null ? 0 : this.version.hashCode());
        return result1;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            JarInfo other = (JarInfo) obj;
            if (this.attributes == null) {
                if (other.attributes != null) {
                    return false;
                }
            } else if (!this.attributes.equals(other.attributes)) {
                return false;
            }

            if (this.version == null) {
                if (other.version != null) {
                    return false;
                }
            } else if (!this.version.equals(other.version)) {
                return false;
            }

            return true;
        }
    }
}
