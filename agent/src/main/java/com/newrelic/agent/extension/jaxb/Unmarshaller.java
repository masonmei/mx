package com.newrelic.agent.extension.jaxb;

import java.text.MessageFormat;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import com.newrelic.deps.com.google.common.collect.ImmutableMap;
import com.newrelic.deps.com.google.common.collect.Maps;

public abstract class Unmarshaller<T> {
    private static final Map<Class<?>, Unmarshaller<?>> DEFAULT_UNMARSHALLERS = createDefaultUnmarshallers();
    private final Class<?> type;

    public Unmarshaller(Class<?> type) {
        this.type = type;
    }

    private static Map<Class<?>, Unmarshaller<?>> createDefaultUnmarshallers() {
        Map unmarshallers = Maps.newHashMap();

        unmarshallers.put(String.class, new Unmarshaller(String.class) {
            public String unmarshall(Node node) {
                if (node.getNodeType() == 1) {
                    Node firstChild = node.getFirstChild();
                    if (firstChild == null) {
                        throw new DOMException((short) 8, MessageFormat
                                                                  .format("The element node {0}  is present, but is "
                                                                                  + "empty.",
                                                                                 new Object[] {node.getNodeName()}));
                    }

                    return node.getFirstChild().getNodeValue();
                }

                return node.getNodeValue();
            }
        });
        unmarshallers.put(Boolean.class, new Unmarshaller(Boolean.class) {
            public Boolean unmarshall(Node node) {
                return Boolean.valueOf(node.getNodeValue());
            }
        });
        unmarshallers.put(Double.class, new Unmarshaller(Double.class) {
            public Double unmarshall(Node node) {
                return Double.valueOf(node.getNodeValue());
            }
        });
        unmarshallers.put(Long.class, new Unmarshaller(Long.class) {
            public Long unmarshall(Node node) {
                return Long.valueOf(node.getNodeValue());
            }
        });
        unmarshallers.put(Integer.class, new Unmarshaller(Integer.class) {
            public Integer unmarshall(Node node) {
                return Integer.valueOf(node.getNodeValue());
            }
        });
        return ImmutableMap.copyOf(unmarshallers);
    }

    public static Map<? extends Class<?>, ? extends Unmarshaller<?>> getDefaultUnmarshallers() {
        return DEFAULT_UNMARSHALLERS;
    }

    public abstract T unmarshall(Node paramNode) throws UnmarshalException;

    public String toString() {
        return "Unmarshaller [type=" + type + "]";
    }
}