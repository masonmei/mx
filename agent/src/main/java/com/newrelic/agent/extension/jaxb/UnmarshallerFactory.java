//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.extension.jaxb;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.deps.com.google.common.collect.Maps;

public class UnmarshallerFactory {
    static final Map<Class<?>, Unmarshaller<?>> cachedUnmarshallers = Maps.newConcurrentMap();

    public UnmarshallerFactory() {
    }

    public static <T> Unmarshaller<T> create(final Class<T> clazz) throws UnmarshalException {
        Unmarshaller cachedUnmarshaller = (Unmarshaller) cachedUnmarshallers.get(clazz);
        if (cachedUnmarshaller != null) {
            return cachedUnmarshaller;
        } else {
            final Unmarshaller<T> unmarshaller = create(clazz, Maps.newHashMap(Unmarshaller.getDefaultUnmarshallers()));
            Unmarshaller newUnmarshaller = new Unmarshaller<T>(clazz) {
                public T unmarshall(Node node) throws UnmarshalException {
                    return unmarshaller.unmarshall(((Document) node).getDocumentElement());
                }
            };
            cachedUnmarshallers.put(clazz, newUnmarshaller);
            return newUnmarshaller;
        }
    }

    private static <T> Unmarshaller<T> create(final Class<T> clazz, Map<Class<?>, Unmarshaller<?>> unmarshallers)
            throws UnmarshalException {
        try {
            final UnmarshallerFactory.Setter e = getAttributesSetter(clazz, unmarshallers);
            final UnmarshallerFactory.Setter childrenSetter = getChildSetter(clazz, unmarshallers);
            Unmarshaller<T> classUnmarshaller = new Unmarshaller<T>(clazz) {
                public T unmarshall(Node node) throws UnmarshalException {
                    try {
                        T ex = clazz.newInstance();
                        e.set(ex, node);
                        childrenSetter.set(ex, node);
                        return ex;
                    } catch (InstantiationException var3) {
                        throw new UnmarshalException(var3);
                    } catch (IllegalAccessException var4) {
                        throw new UnmarshalException(var4);
                    }
                }
            };
            unmarshallers.put(clazz, classUnmarshaller);
            return classUnmarshaller;
        } catch (InstantiationException var5) {
            throw new UnmarshalException(var5);
        } catch (IllegalAccessException var6) {
            throw new UnmarshalException(var6);
        }
    }

    private static UnmarshallerFactory.Setter getChildSetter(final Class<?> clazz,
                                                             Map<Class<?>, Unmarshaller<?>> unmarshallers)
            throws InstantiationException, IllegalAccessException, UnmarshalException {
        final HashMap childSetters = Maps.newHashMap();
        XmlType type = (XmlType) clazz.getAnnotation(XmlType.class);
        if (type != null) {
            String[] arr$ = type.propOrder();
            int len$ = arr$.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String t = arr$[i$];
                if (!t.isEmpty()) {
                    try {
                        Field e = clazz.getDeclaredField(t);
                        e.setAccessible(true);
                        Class fieldType = e.getType();
                        Unmarshaller unmarshaller = getUnmarshaller(unmarshallers, fieldType, e);
                        XmlValue xmlValue = (XmlValue) e.getAnnotation(XmlValue.class);
                        if (xmlValue != null) {
                            if (type.propOrder().length > 1) {
                                throw new UnmarshalException(clazz.getName()
                                                                     + " has an @XmlValue field so only one child "
                                                                     + "type was expected, but multiple were found: "
                                                                     + type.propOrder());
                            }

                            return new UnmarshallerFactory.ChildSetter(t, unmarshaller, e);
                        }

                        childSetters.put(t, new UnmarshallerFactory.ChildSetter(t, unmarshaller, e));
                    } catch (Exception var12) {
                        throw new UnmarshalException(var12);
                    }
                }
            }
        }

        return new UnmarshallerFactory.Setter() {
            public void set(Object obj, Node node)
                    throws IllegalArgumentException, IllegalAccessException, InstantiationException,
                                   UnmarshalException {
                NodeList childNodes = node.getChildNodes();

                for (int i = 0; i < childNodes.getLength(); ++i) {
                    Node item = childNodes.item(i);
                    if (item.getNodeType() != 8) {
                        String nodeName = item.getNodeName();
                        String prefix = item.getPrefix();
                        if (prefix != null && !prefix.isEmpty()) {
                            nodeName = nodeName.substring(prefix.length() + 1);
                        }

                        UnmarshallerFactory.Setter setter = (UnmarshallerFactory.Setter) childSetters.get(nodeName);
                        if (setter == null) {
                            throw new UnmarshalException("No setter for node name " + nodeName + " on "
                                                                 + clazz.getName());
                        }

                        setter.set(obj, item);
                    }
                }

            }
        };
    }

    private static UnmarshallerFactory.Setter getAttributesSetter(Class<?> clazz,
                                                                  Map<Class<?>, Unmarshaller<?>> unmarshallers)
            throws InstantiationException, IllegalAccessException, UnmarshalException {
        final ArrayList attributeSetters = Lists.newArrayList();
        Field[] arr$ = clazz.getDeclaredFields();
        int len$ = arr$.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            Field field = arr$[i$];
            XmlAttribute attribute = (XmlAttribute) field.getAnnotation(XmlAttribute.class);
            if (attribute != null) {
                field.setAccessible(true);
                Class declaringClass = field.getType();
                Unmarshaller unmarshaller = getUnmarshaller(unmarshallers, declaringClass, field);
                attributeSetters.add(new UnmarshallerFactory.AttributeSetter(attribute.name(), unmarshaller, field));
            }
        }

        return new UnmarshallerFactory.Setter() {
            public void set(Object obj, Node node)
                    throws IllegalArgumentException, IllegalAccessException, InstantiationException,
                                   UnmarshalException {
                if (node.getAttributes() != null) {
                    Iterator i$ = attributeSetters.iterator();

                    while (i$.hasNext()) {
                        UnmarshallerFactory.Setter setter = (UnmarshallerFactory.Setter) i$.next();
                        setter.set(obj, node);
                    }
                }

            }
        };
    }

    private static Unmarshaller<?> getUnmarshaller(Map<Class<?>, Unmarshaller<?>> unmarshallers, Class<?> clazz,
                                                   Field field)
            throws InstantiationException, IllegalAccessException, UnmarshalException {
        if (clazz.isAssignableFrom(List.class)) {
            ParameterizedType unmarshaller = (ParameterizedType) field.getGenericType();
            clazz = (Class) unmarshaller.getActualTypeArguments()[0];
        }

        Unmarshaller unmarshaller1 = (Unmarshaller) unmarshallers.get(clazz);
        if (unmarshaller1 == null) {
            unmarshaller1 = create(clazz, unmarshallers);
            unmarshallers.put(clazz, unmarshaller1);
        }

        return unmarshaller1;
    }

    private interface Setter {
        void set(Object var1, Node var2)
                throws IllegalArgumentException, IllegalAccessException, InstantiationException, UnmarshalException;
    }

    private static class AttributeSetter implements UnmarshallerFactory.Setter {
        private final String name;
        private final Unmarshaller<?> unmarshaller;
        private final Field field;

        public AttributeSetter(String name, Unmarshaller<?> unmarshaller, Field field) {
            this.name = name;
            this.unmarshaller = unmarshaller;
            this.field = field;
        }

        public void set(Object obj, Node node)
                throws IllegalArgumentException, IllegalAccessException, InstantiationException, UnmarshalException {
            Node namedItem = node.getAttributes().getNamedItem(this.name);
            if (namedItem != null) {
                Object value = this.unmarshaller.unmarshall(namedItem);
                if (value != null) {
                    this.field.set(obj, value);
                }
            }

        }

        public String toString() {
            return "AttributeSetter [name=" + this.name + "]";
        }
    }

    private static class ChildSetter implements UnmarshallerFactory.Setter {
        private final Unmarshaller<?> unmarshaller;
        private final Field field;

        public ChildSetter(String t, Unmarshaller<?> unmarshaller, Field field) {
            this.unmarshaller = unmarshaller;
            this.field = field;
        }

        public void set(Object obj, Node node)
                throws IllegalArgumentException, IllegalAccessException, InstantiationException, UnmarshalException {
            Object value = this.unmarshaller.unmarshall(node);
            if (value != null) {
                if (this.field.getType().isAssignableFrom(List.class)) {
                    Object list = (List) this.field.get(obj);
                    if (list == null) {
                        list = Lists.newArrayList();
                        this.field.set(obj, list);
                    }

                    ((List) list).add(value);
                } else {
                    this.field.set(obj, value);
                }
            }

        }

        public String toString() {
            return "ChildSetter [field=" + this.field + "]";
        }
    }
}
