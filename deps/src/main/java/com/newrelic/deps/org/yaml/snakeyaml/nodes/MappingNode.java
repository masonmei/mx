/**
 * Copyright (c) 2008-2009 Andrey Somov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newrelic.deps.org.yaml.snakeyaml.nodes;

import java.util.List;

import com.newrelic.deps.org.yaml.snakeyaml.error.Mark;

/**
 * @see <a href="http://pyyaml.org/wiki/PyYAML">PyYAML</a> for more information
 */
public class MappingNode extends CollectionNode {
    private Class<? extends Object> keyType;
    private Class<? extends Object> valueType;
    private List<NodeTuple> value;

    public MappingNode(String tag, List<NodeTuple> value, Mark startMark, Mark endMark,
            Boolean flowStyle) {
        super(tag, startMark, endMark, flowStyle);
        if (value == null) {
            throw new NullPointerException("value in a Node is required.");
        }
        this.value = value;
        keyType = Object.class;
        valueType = Object.class;
    }

    public MappingNode(String tag, List<NodeTuple> value, Boolean flowStyle) {
        this(tag, value, null, null, flowStyle);
    }

    @Override
    public NodeId getNodeId() {
        return NodeId.mapping;
    }

    public List<NodeTuple> getValue() {
        for (NodeTuple nodes : value) {
            nodes.getKeyNode().setType(keyType);
            nodes.getValueNode().setType(valueType);
        }
        return value;
    }

    public void setValue(List<NodeTuple> merge) {
        value = merge;
    }

    public void setKeyType(Class<? extends Object> keyType) {
        this.keyType = keyType;
    }

    public void setValueType(Class<? extends Object> valueType) {
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        String values;
        StringBuffer buf = new StringBuffer();
        for (NodeTuple node : getValue()) {
            buf.append("{ key=");
            buf.append(node.getKeyNode());
            buf.append("; value=Node<");
            // to avoid overflow in case of recursive structures
            buf.append(System.identityHashCode(node.getValueNode()));
            buf.append("> }");
        }
        values = buf.toString();
        return "<" + this.getClass().getName() + " (tag=" + getTag() + ", values=" + values + ")>";
    }
}
