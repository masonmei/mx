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
package com.newrelic.deps.org.yaml.snakeyaml.constructor;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.newrelic.deps.org.yaml.snakeyaml.composer.Composer;
import com.newrelic.deps.org.yaml.snakeyaml.composer.ComposerException;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.MappingNode;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.Node;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.NodeId;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.NodeTuple;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.ScalarNode;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.SequenceNode;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.Tags;

/**
 * @see <a href="http://pyyaml.org/wiki/PyYAML">PyYAML</a> for more information
 */
public abstract class BaseConstructor {
    /**
     * It maps the node kind to the the Construct implementation. When the
     * runtime class is known then the implicit tag is ignored.
     */
    protected final Map<NodeId, Construct> yamlClassConstructors = new EnumMap<NodeId, Construct>(
            NodeId.class);
    /**
     * It maps the (explicit or implicit) tag to the Construct implementation.
     * It is used: <br/>
     * 1) explicit tag - if present. <br/>
     * 2) implicit tag - when the runtime class of the instance is unknown (the
     * node has the Object.class)
     */
    protected final Map<String, Construct> yamlConstructors = new HashMap<String, Construct>();

    private Composer composer;
    private final Map<Node, Object> constructedObjects;
    private final Set<Node> recursiveObjects;
    private final Stack<Tuple<Node, Object>> toBeConstructedAt2ndStep;
    private final LinkedList<Tuple<Map<Object, Object>, Tuple<Object, Object>>> maps2fill;
    private final LinkedList<Tuple<Set<Object>, Object>> sets2fill;

    protected Class<? extends Object> rootType;

    public BaseConstructor() {
        constructedObjects = new HashMap<Node, Object>();
        recursiveObjects = new HashSet<Node>();
        toBeConstructedAt2ndStep = new Stack<Tuple<Node, Object>>();
        maps2fill = new LinkedList<Tuple<Map<Object, Object>, Tuple<Object, Object>>>();
        sets2fill = new LinkedList<Tuple<Set<Object>, Object>>();
        rootType = Object.class;
    }

    public void setComposer(Composer composer) {
        this.composer = composer;
    }

    /**
     * Check if more documents available
     * 
     * @return true when there are more YAML documents in the stream
     */
    public boolean checkData() {
        // If there are more documents available?
        return composer.checkNode();
    }

    /**
     * Construct and return the next document
     * 
     * @return constructed instance
     */
    public Object getData() {
        // Construct and return the next document.
        composer.checkNode();
        Node node = composer.getNode();
        node.setType(rootType);
        return constructDocument(node);
    }

    /**
     * Ensure that the stream contains a single document and construct it
     * 
     * @return constructed instance
     * @throws ComposerException
     *             in case there are more documents in the stream
     */
    public Object getSingleData() {
        // Ensure that the stream contains a single document and construct it
        Node node = composer.getSingleNode();
        if (node != null) {
            node.setType(rootType);
            return constructDocument(node);
        }
        return null;
    }

    /**
     * Construct complete YAML document. Call the second step in case of
     * recursive structures. At the end cleans all the state.
     * 
     * @param node
     *            root Node
     * @return Java instance
     */
    private Object constructDocument(Node node) {
        Object data = constructObject(node);
        while (!toBeConstructedAt2ndStep.isEmpty()) {
            Tuple<Node, Object> toBeProcessed = toBeConstructedAt2ndStep.pop();
            callPostCreate(toBeProcessed._1(), toBeProcessed._2());
        }
        if (!maps2fill.isEmpty()) {
            for (Tuple<Map<Object, Object>, Tuple<Object, Object>> entry : maps2fill) {
                Tuple<Object, Object> key_value = entry._2();
                entry._1().put(key_value._1(), key_value._2());
            }
            maps2fill.clear();
        }
        if (!sets2fill.isEmpty()) {
            for (Tuple<Set<Object>, Object> value : sets2fill) {
                value._1().add(value._2());
            }
            sets2fill.clear();
        }
        constructedObjects.clear();
        recursiveObjects.clear();
        toBeConstructedAt2ndStep.clear();
        return data;
    }

    /**
     * Construct object from the specified Node. Return existing instance if the
     * node is already constructed.
     * 
     * @param node
     *            Node to be constructed
     * @return Java instance
     */
    protected Object constructObject(Node node) {
        if (constructedObjects.containsKey(node)) {
            return constructedObjects.get(node);
        }
        if (recursiveObjects.contains(node)) {
            throw new ConstructorException(null, null, "found unconstructable recursive node", node
                    .getStartMark());
        }
        recursiveObjects.add(node);
        Object data = callConstructor(node);
        if (node.isTwoStepsConstruction()) {
            toBeConstructedAt2ndStep.push(new Tuple<Node, Object>(node, data));
        }
        constructedObjects.put(node, data);
        recursiveObjects.remove(node);
        return data;
    }

    /**
     * Create Java instance from the specified Node
     * 
     * @param node
     *            Node to be constructed
     * @return Java instance
     */
    protected Object callConstructor(Node node) {
        return getConstructor(node).construct(node);
    }

    protected void callPostCreate(Node node, Object object) {
        getConstructor(node).construct2ndStep(node, object);
    }

    /**
     * Get the constructor to construct the Node. For implicit tags if the
     * runtime class is known a dedicated Construct implementation is used.
     * Otherwise the constructor is chosen by the tag.
     * 
     * @param node
     *            Node to be constructed
     * @return Construct implementation for the specified node
     */
    private Construct getConstructor(Node node) {
        if (!node.hasExplicitTag() && !Object.class.equals(node.getType())
                && !node.getTag().equals(Tags.NULL)) {
            return yamlClassConstructors.get(node.getNodeId());
        } else {
            Construct constructor = yamlConstructors.get(node.getTag());
            if (constructor == null) {
                return yamlConstructors.get(null);
            }
            return constructor;
        }
    }

    protected Object constructScalar(ScalarNode node) {
        return node.getValue();
    }

    protected List<Object> createDefaultList(int initSize) {
        return new LinkedList<Object>();
    }

    protected List<? extends Object> constructSequence(SequenceNode node) {
        List<Object> result = createDefaultList(node.getValue().size());
        constructSequenceStep2(node, result);
        return result;
    }

    protected void constructSequenceStep2(SequenceNode node, List<Object> list) {
        for (Node child : node.getValue()) {
            list.add(constructObject(child));
        }
    }

    protected Map<Object, Object> createDefaultMap() {
        // respect order from YAML document
        return new LinkedHashMap<Object, Object>();
    }

    protected Set<Object> createDefaultSet() {
        // respect order from YAML document
        return new LinkedHashSet<Object>();
    }

    protected Set<Object> constructSet(MappingNode node) {
        Set<Object> set = createDefaultSet();
        constructSet2ndStep(node, set);
        return set;
    }

    protected Map<Object, Object> constructMapping(MappingNode node) {
        Map<Object, Object> mapping = createDefaultMap();
        constructMapping2ndStep(node, mapping);
        return mapping;
    }

    protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
        List<NodeTuple> nodeValue = (List<NodeTuple>) node.getValue();
        for (NodeTuple tuple : nodeValue) {
            Node keyNode = tuple.getKeyNode();
            Node valueNode = tuple.getValueNode();
            Object key = constructObject(keyNode);
            if (key != null) {
                try {
                    key.hashCode();// check circular dependencies
                } catch (Exception e) {
                    throw new ConstructorException("while constructing a mapping", node
                            .getStartMark(), "found unacceptable key " + key, tuple.getKeyNode()
                            .getStartMark(), e);
                }
            }
            Object value = constructObject(valueNode);
            if (keyNode.isTwoStepsConstruction()) {
                /*
                 * if keyObject is created it 2 steps we should postpone putting
                 * it in map because it may have different hash after
                 * initialization compared to clean just created one. And map of
                 * course does not observe key hashCode changes.
                 */
                maps2fill.addFirst(new Tuple<Map<Object, Object>, Tuple<Object, Object>>(mapping,
                        new Tuple<Object, Object>(key, value)));
            } else {
                mapping.put(key, value);
            }
        }
    }

    protected void constructSet2ndStep(MappingNode node, Set<Object> set) {
        List<NodeTuple> nodeValue = (List<NodeTuple>) node.getValue();
        for (NodeTuple tuple : nodeValue) {
            Node keyNode = tuple.getKeyNode();
            Object key = constructObject(keyNode);
            if (key != null) {
                try {
                    key.hashCode();// check circular dependencies
                } catch (Exception e) {
                    throw new ConstructorException("while constructing a Set", node.getStartMark(),
                            "found unacceptable key " + key, tuple.getKeyNode().getStartMark(), e);
                }
            }
            if (keyNode.isTwoStepsConstruction()) {
                /*
                 * if keyObject is created it 2 steps we should postpone putting
                 * it into the set because it may have different hash after
                 * initialization compared to clean just created one. And set of
                 * course does not observe value hashCode changes.
                 */
                sets2fill.addFirst(new Tuple<Set<Object>, Object>(set, key));
            } else {
                set.add(key);
            }
        }
    }

    // TODO protected List<Object[]> constructPairs(MappingNode node) {
    // List<Object[]> pairs = new LinkedList<Object[]>();
    // List<Node[]> nodeValue = (List<Node[]>) node.getValue();
    // for (Iterator<Node[]> iter = nodeValue.iterator(); iter.hasNext();) {
    // Node[] tuple = iter.next();
    // Object key = constructObject(Object.class, tuple[0]);
    // Object value = constructObject(Object.class, tuple[1]);
    // pairs.add(new Object[] { key, value });
    // }
    // return pairs;
    // }
}
