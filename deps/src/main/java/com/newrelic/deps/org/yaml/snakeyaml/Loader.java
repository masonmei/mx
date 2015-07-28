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
package com.newrelic.deps.org.yaml.snakeyaml;

import java.util.Iterator;

import com.newrelic.deps.org.yaml.snakeyaml.composer.Composer;
import com.newrelic.deps.org.yaml.snakeyaml.constructor.BaseConstructor;
import com.newrelic.deps.org.yaml.snakeyaml.constructor.Constructor;
import com.newrelic.deps.org.yaml.snakeyaml.error.YAMLException;
import com.newrelic.deps.org.yaml.snakeyaml.events.Event;
import com.newrelic.deps.org.yaml.snakeyaml.nodes.Node;
import com.newrelic.deps.org.yaml.snakeyaml.parser.Parser;
import com.newrelic.deps.org.yaml.snakeyaml.parser.ParserImpl;
import com.newrelic.deps.org.yaml.snakeyaml.reader.Reader;
import com.newrelic.deps.org.yaml.snakeyaml.resolver.Resolver;

/**
 * @see <a href="http://pyyaml.org/wiki/PyYAML">PyYAML</a> for more information
 */
public class Loader {
    protected final BaseConstructor constructor;
    protected Resolver resolver;
    private boolean attached = false;

    public Loader(BaseConstructor constructor) {
        super();
        this.constructor = constructor;
    }

    public Loader() {
        this(new Constructor());
    }

    public Object load(java.io.Reader io) {
        Composer composer = new Composer(new ParserImpl(new Reader(io)), resolver);
        constructor.setComposer(composer);
        return constructor.getSingleData();
    }

    public Iterable<Object> loadAll(java.io.Reader yaml) {
        Composer composer = new Composer(new ParserImpl(new Reader(yaml)), resolver);
        this.constructor.setComposer(composer);
        Iterator<Object> result = new Iterator<Object>() {
            public boolean hasNext() {
                return constructor.checkData();
            }

            public Object next() {
                return constructor.getData();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new YamlIterable(result);
    }

    /**
     * Parse the first YAML document in a stream and produce the corresponding
     * representation tree.
     * 
     * @param yaml
     *            YAML document
     * @return parsed root Node for the specified YAML document
     */
    public Node compose(java.io.Reader yaml) {
        Composer composer = new Composer(new ParserImpl(new Reader(yaml)), resolver);
        this.constructor.setComposer(composer);
        return composer.getSingleNode();
    }

    /**
     * Parse all YAML documents in a stream and produce corresponding
     * representation trees.
     * 
     * @param yaml
     *            stream of YAML documents
     * @return parsed root Nodes for all the specified YAML documents
     */
    public Iterable<Node> composeAll(java.io.Reader yaml) {
        final Composer composer = new Composer(new ParserImpl(new Reader(yaml)), resolver);
        this.constructor.setComposer(composer);
        Iterator<Node> result = new Iterator<Node>() {
            public boolean hasNext() {
                return composer.checkNode();
            }

            public Node next() {
                return composer.getNode();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new NodeIterable(result);
    }

    private class NodeIterable implements Iterable<Node> {
        private Iterator<Node> iterator;

        public NodeIterable(Iterator<Node> iterator) {
            this.iterator = iterator;
        }

        public Iterator<Node> iterator() {
            return iterator;
        }

    }

    private class YamlIterable implements Iterable<Object> {
        private Iterator<Object> iterator;

        public YamlIterable(Iterator<Object> iterator) {
            this.iterator = iterator;
        }

        public Iterator<Object> iterator() {
            return iterator;
        }

    }

    public void setResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Because Loader is stateful it cannot be shared
     */
    void setAttached() {
        if (!attached) {
            attached = true;
        } else {
            throw new YAMLException("Loader cannot be shared.");
        }
    }

    /**
     * Parse a YAML stream and produce parsing events.
     * 
     * @param yaml
     *            YAML document(s)
     * @return parsed events
     */
    public Iterable<Event> parse(java.io.Reader yaml) {
        final Parser parser = new ParserImpl(new Reader(yaml));
        Iterator<Event> result = new Iterator<Event>() {
            public boolean hasNext() {
                return parser.peekEvent() != null;
            }

            public Event next() {
                return parser.getEvent();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return new EventIterable(result);
    }

    private class EventIterable implements Iterable<Event> {
        private Iterator<Event> iterator;

        public EventIterable(Iterator<Event> iterator) {
            this.iterator = iterator;
        }

        public Iterator<Event> iterator() {
            return iterator;
        }
    }
}
