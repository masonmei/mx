//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation;

import java.lang.reflect.InvocationHandler;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

import com.newrelic.agent.bridge.AgentBridge;

public class MethodBuilder {
    public static final Object LOAD_THIS = new Object();
    public static final Object LOAD_ARG_ARRAY = new Object();
    static final String INVOCATION_HANDLER_FIELD_NAME = "__nr__InvocationHandlers";
    static final Type INVOCATION_HANDLER_ARRAY_TYPE = Type.getType(InvocationHandler[].class);
    static final Type INVOCATION_HANDLER_TYPE = Type.getType(InvocationHandler.class);
    static final Method INVOCATION_HANDLER_INVOKE_METHOD =
            new Method("invoke", "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;");
    private static final Map<Type, Type> primitiveToObjectType = Collections.unmodifiableMap(new HashMap() {
        private static final long serialVersionUID = 1L;

        {
            this.put(Type.BOOLEAN_TYPE, Type.getType(Boolean.class));
            this.put(Type.BYTE_TYPE, Type.getType(Byte.class));
            this.put(Type.CHAR_TYPE, Type.getType(Character.class));
            this.put(Type.DOUBLE_TYPE, Type.getType(Double.class));
            this.put(Type.FLOAT_TYPE, Type.getType(Float.class));
            this.put(Type.INT_TYPE, Type.getType(Integer.class));
            this.put(Type.LONG_TYPE, Type.getType(Long.class));
            this.put(Type.SHORT_TYPE, Type.getType(Short.class));
        }
    });
    private final GeneratorAdapter mv;
    private final int access;

    public MethodBuilder(GeneratorAdapter mv, int access) {
        this.mv = mv;
        this.access = access;
    }

    public static Type getBoxedType(Type type) {
        return (Type) primitiveToObjectType.get(type);
    }

    public GeneratorAdapter getGeneratorAdapter() {
        return this.mv;
    }

    public MethodBuilder loadInvocationHandlerFromProxy() {
        this.mv.getStatic(Type.getType(AgentBridge.class), "agentHandler", INVOCATION_HANDLER_TYPE);
        return this;
    }

    public MethodBuilder invokeInvocationHandlerInterface(boolean popTheReturnValue) {
        this.mv.invokeInterface(INVOCATION_HANDLER_TYPE, INVOCATION_HANDLER_INVOKE_METHOD);
        if (popTheReturnValue) {
            this.mv.pop();
        }

        return this;
    }

    public MethodBuilder loadInvocationHandlerProxyAndMethod(Object value) {
        this.pushAndBox(value);
        this.mv.visitInsn(1);
        return this;
    }

    public MethodBuilder loadArray(Class<?> arrayClass, Object... objects) {
        if (objects != null && objects.length != 0) {
            this.mv.push(objects.length);
            Type objectType = Type.getType(arrayClass);
            this.mv.newArray(objectType);

            for (int i = 0; i < objects.length; ++i) {
                this.mv.dup();
                this.mv.push(i);
                if (LOAD_THIS == objects[i]) {
                    if (this.isStatic()) {
                        this.mv.visitInsn(1);
                    } else {
                        this.mv.loadThis();
                    }
                } else if (LOAD_ARG_ARRAY == objects[i]) {
                    this.mv.loadArgArray();
                } else if (objects[i] instanceof Runnable) {
                    ((Runnable) objects[i]).run();
                } else {
                    this.pushAndBox(objects[i]);
                }

                this.mv.arrayStore(objectType);
            }

            return this;
        } else {
            this.mv.visitInsn(1);
            return this;
        }
    }

    private boolean isStatic() {
        return (this.access & 8) != 0;
    }

    public MethodBuilder pushAndBox(Object value) {
        if (value == null) {
            this.mv.visitInsn(1);
        } else if (value instanceof Boolean) {
            this.mv.push(((Boolean) value).booleanValue());
            this.mv.box(Type.BOOLEAN_TYPE);
        } else if (value instanceof Integer) {
            this.mv.visitIntInsn(17, ((Integer) value).intValue());
            this.mv.box(Type.INT_TYPE);
        } else {
            this.mv.visitLdcInsn(value);
        }

        return this;
    }

    public MethodBuilder loadSuccessful() {
        this.loadInvocationHandlerProxyAndMethod("s");
        return this;
    }

    public MethodBuilder loadUnsuccessful() {
        this.loadInvocationHandlerProxyAndMethod("u");
        return this;
    }

    public Type box(Type type) {
        if (type.getSort() != 10 && type.getSort() != 9) {
            Type boxed = getBoxedType(type);
            this.mv.invokeStatic(boxed, new Method("valueOf", boxed, new Type[] {type}));
            return boxed;
        } else {
            return type;
        }
    }
}
