package com.newrelic.agent.instrumentation.api;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import com.newrelic.deps.com.google.common.collect.ImmutableMap;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.commons.Method;
import com.newrelic.deps.org.objectweb.asm.tree.MethodNode;

public class DefaultApiImplementations {
    private final Map<String, Map<Method, MethodNode>> interfaceToMethods;

    public DefaultApiImplementations() throws Exception {
        this(new Class[] {DefaultRequest.class, DefaultResponse.class});
    }

    public DefaultApiImplementations(Class<?>[] defaultImplementations) throws Exception {
        Map interfaceToMethods = Maps.newHashMap();
        for (Class clazz : defaultImplementations) {
            if (Modifier.isAbstract(clazz.getModifiers())) {
                throw new Exception(clazz.getName() + " cannot be abstract");
            }
            final ClassReader reader = Utils.readClass(clazz);
            String[] interfaces = reader.getInterfaces();
            if (interfaces.length != 1) {
                throw new Exception(clazz.getName() + " implements multiple interfaces: " + Arrays.asList(interfaces));
            }
            final Map methods = Maps.newHashMap();
            interfaceToMethods.put(interfaces[0], methods);

            ClassVisitor cv = new ClassVisitor(Agent.ASM_LEVEL) {
                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                                 String[] exceptions) {
                    Method method = new Method(name, desc);
                    if ((access & 0x10) != 0) {
                        Agent.LOG.severe("Default implementation " + reader.getClassName() + " should not declared "
                                                 + method + " final");

                        return null;
                    }
                    MethodNode node = new MethodNode(access, name, desc, signature, exceptions);
                    methods.put(method, node);
                    return node;
                }
            };
            reader.accept(cv, 2);
            methods.remove(new Method("<init>", "()V"));
            methods.remove(new Method("<cinit>", "()V"));
        }
        this.interfaceToMethods = ImmutableMap.copyOf(interfaceToMethods);
    }

    public Map<String, Map<Method, MethodNode>> getApiClassNameToDefaultMethods() {
        return interfaceToMethods;
    }

    private static final class DefaultResponse implements Response {
        public int getStatus() throws Exception {
            return 0;
        }

        public String getStatusMessage() throws Exception {
            return null;
        }

        public void setHeader(String name, String value) {
        }

        public String getContentType() {
            return null;
        }

        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }

    private static final class DefaultRequest implements Request {
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }

        public String getHeader(String name) {
            return null;
        }

        public String getRequestURI() {
            return null;
        }

        public String getRemoteUser() {
            return null;
        }

        public Enumeration<?> getParameterNames() {
            return null;
        }

        public String[] getParameterValues(String name) {
            return null;
        }

        public Object getAttribute(String name) {
            return null;
        }

        public String getCookieValue(String name) {
            return null;
        }
    }
}