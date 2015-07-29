package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Writer;

import com.newrelic.deps.org.json.simple.JSONAware;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import com.newrelic.deps.org.json.simple.JSONValue;

public interface ThreadType extends JSONStreamAware, JSONAware {
    String getName();

    enum BasicThreadType implements ThreadType {
        AGENT("agent"),
        AGENT_INSTRUMENTATION("agent_instrumentation"),
        REQUEST("request"), BACKGROUND("background"), OTHER("other");

        private String name;

        private BasicThreadType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void writeJSONString(Writer out) throws IOException {
            JSONValue.writeJSONString(name, out);
        }

        public String toJSONString() {
            return name;
        }
    }
}