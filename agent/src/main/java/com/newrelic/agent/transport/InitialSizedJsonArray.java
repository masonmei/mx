package com.newrelic.agent.transport;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

public class InitialSizedJsonArray implements JSONStreamAware {
    private List<Object> toSend;

    public InitialSizedJsonArray(int size) {
        if (size > 0) {
            toSend = new ArrayList(size);
        } else {
            toSend = Collections.emptyList();
        }
    }

    public void add(Object obj) {
        toSend.add(obj);
    }

    public void addAll(Collection<Object> objs) {
        toSend.addAll(objs);
    }

    public int size() {
        return toSend.size();
    }

    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(toSend, out);
    }
}