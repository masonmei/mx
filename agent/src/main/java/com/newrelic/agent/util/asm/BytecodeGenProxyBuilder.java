package com.newrelic.agent.util.asm;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.AgentError;

public class BytecodeGenProxyBuilder<T> {
    private final Class<T> target;
    private final GeneratorAdapter methodAdapter;
    private final boolean loadArguments;
    private final Variables variables;
    private Map<Object, Runnable> arguments;
    private Map<Type, VariableLoader> loaders;

    private BytecodeGenProxyBuilder(Class<T> target, GeneratorAdapter methodAdapter, boolean loadArguments) {
        this.target = target;
        this.methodAdapter = methodAdapter;
        variables = new VariableLoaderImpl();
        this.loadArguments = loadArguments;
    }

    public static <T> BytecodeGenProxyBuilder<T> newBuilder(Class<T> target, GeneratorAdapter methodAdapter,
                                                            boolean loadArguments) {
        return new BytecodeGenProxyBuilder(target, methodAdapter, loadArguments);
    }

    public Variables getVariables() {
        return variables;
    }

    private BytecodeGenProxyBuilder<T> addArgument(Object instance, Runnable runnable) {
        if (arguments == null) {
            arguments = Maps.newHashMap();
        }
        if (runnable == null) {
            throw new AgentError("Runnable was null");
        }
        arguments.put(instance, runnable);
        return this;
    }

    public BytecodeGenProxyBuilder<T> addLoader(Type t, VariableLoader loader) {
        if (loaders == null) {
            loaders = Maps.newHashMap();
        }
        loaders.put(t, loader);
        return this;
    }

    private Map<Type, VariableLoader> getLoaders() {
        return loaders == null ? Collections.EMPTY_MAP : loaders;
    }

    private Map<Object, Runnable> getArguments() {
        return arguments == null ? Collections.EMPTY_MAP : arguments;
    }

    public T build() {
        InvocationHandler handler = new InvocationHandler() {
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                org.objectweb.asm.commons.Method m = org.objectweb.asm.commons.Method.getMethod(method);

                if (loadArguments) {
                    for (int i = 0; i < m.getArgumentTypes().length; i++) {
                        Object value = args[i];
                        Type type = m.getArgumentTypes()[i];

                        load(type, value);
                    }
                }

                try {
                    getMethodVisitor()
                            .visitMethodInsn(185, Type.getInternalName(target), m.getName(), m.getDescriptor());
                } catch (ArrayIndexOutOfBoundsException e) {
                    Agent.LOG.log(Level.FINER, "Error invoking {0}.{1}", new Object[] {target.getName(), m});
                    throw e;
                }
                return dummyReturnValue(m.getReturnType());
            }

            private Object dummyReturnValue(Type returnType) {
                switch (returnType.getSort()) {
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        return Integer.valueOf(0);
                    case 7:
                        return Long.valueOf(0L);
                    case 6:
                        return Float.valueOf(0.0F);
                    case 8:
                        return Double.valueOf(0.0D);
                    case 1:
                        return Boolean.valueOf(false);
                }
                return null;
            }

            private MethodVisitor getMethodVisitor() {
                if ((methodAdapter instanceof AdviceAdapter)) {
                    try {
                        Field field = AdviceAdapter.class.getDeclaredField("constructor");
                        field.setAccessible(true);
                        if (field.getBoolean(methodAdapter)) {
                            field = MethodVisitor.class.getDeclaredField("mv");
                            field.setAccessible(true);
                            return (MethodVisitor) field.get(methodAdapter);
                        }
                    } catch (Exception e) {
                        Agent.LOG.log(Level.FINE, e, e.toString(), new Object[0]);
                    }
                }
                return methodAdapter;
            }

            private void load(Type type, Object value) {
                if (value == null) {
                    methodAdapter.visitInsn(1);
                    return;
                }

                VariableLoader loader = (VariableLoader) BytecodeGenProxyBuilder.this.getLoaders().get(type);
                Runnable handler = (Runnable) BytecodeGenProxyBuilder.this.getArguments().get(value);

                if (handler != null) {
                    handler.run();
                } else if (loader != null) {
                    loader.load(value, methodAdapter);
                } else if ((value instanceof LoaderMarker)) {
                    ((LoaderMarker) value).run();
                } else {
                    switch (type.getSort()) {
                        case 10:
                            if ((value instanceof String)) {
                                methodAdapter.push((String) value);
                            } else if (value.getClass().isEnum()) {
                                Enum theEnum = (Enum) value;
                                methodAdapter.getStatic(type, theEnum.name(), type);
                            } else if ((value instanceof Runnable)) {
                                ((Runnable) value).run();
                            } else {
                                throw new AgentError("Unsupported type " + type);
                            }
                            return;
                        case 1:
                            methodAdapter.push(((Boolean) value).booleanValue());
                            return;
                        case 5:
                            methodAdapter.push(((Number) value).intValue());
                            return;
                        case 7:
                            methodAdapter.push(((Number) value).longValue());
                            return;
                        case 6:
                            methodAdapter.push(((Number) value).floatValue());
                            return;
                        case 8:
                            methodAdapter.push(((Number) value).doubleValue());
                            return;
                        case 3:
                            methodAdapter.push(((Number) value).intValue());
                            return;
                        case 9:
                            int count = Array.getLength(value);
                            methodAdapter.push(count);
                            methodAdapter.newArray(type.getElementType());

                            for (int i = 0; i < count; i++) {
                                methodAdapter.dup();
                                methodAdapter.push(i);

                                load(type.getElementType(), Array.get(value, i));

                                methodAdapter.arrayStore(type.getElementType());
                            }

                            return;
                        case 2:
                        case 4:
                    }
                    throw new AgentError("Unsupported type " + type);
                }
            }
        };
        ClassLoader classLoader = BytecodeGenProxyBuilder.class.getClassLoader();
        return (T) Proxy.newProxyInstance(classLoader, new Class[] {target}, handler);
    }

    public static abstract interface LoaderMarker extends Runnable {
    }

    private static abstract class Handler implements InvocationHandler {
        public final Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            if (method.getName().equals("hashCode")) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if (method.getName().equals("toString")) {
                return toString();
            }
            return doInvoke(proxy, method, args);
        }

        protected abstract Object doInvoke(Object paramObject, java.lang.reflect.Method paramMethod,
                                           Object[] paramArrayOfObject);
    }

    public final class VariableLoaderImpl implements Variables {
        public VariableLoaderImpl() {
        }

        private Runnable loadThis() {
            return new LoaderMarker() {
                public void run() {
                    methodAdapter.visitVarInsn(25, 0);
                }

                public String toString() {
                    return "this";
                }
            };
        }

        public Object loadThis(int access) {
            boolean isStatic = (access & 0x8) == 8;
            return isStatic ? null : loadThis();
        }

        public <N extends Number> N loadLocal(final int local, final Type type, N value) {
            Runnable r = new Runnable() {
                public void run() {
                    methodAdapter.loadLocal(local, type);
                }
            };
            return load(value, r);
        }

        public <N extends Number> N load(N value, Runnable runnable) {
            BytecodeGenProxyBuilder.this.addArgument(value, runnable);
            return value;
        }

        public Transaction loadCurrentTransaction() {
            return (Transaction) load(Transaction.class, new Runnable() {
                public void run() {
                    BridgeUtils.getCurrentTransaction(methodAdapter);
                }

                public String toString() {
                    return Transaction.class.getName() + '.' + "CURRENT";
                }
            });
        }

        public <O> O load(Class<O> clazz, final Runnable runnable) {
            if (clazz.isInterface()) {
                InvocationHandler handler = new Handler() {
                    public Object doInvoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        runnable.run();
                        return null;
                    }

                    public String toString() {
                        return runnable.toString();
                    }
                };
                return (O) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
                                                         new Class[] {clazz, LoaderMarker.class}, handler);
            }
            if (clazz.isArray()) {
                Object key = Array.newInstance(clazz.getComponentType(), 0);
                BytecodeGenProxyBuilder.this.addArgument(key, runnable);
                return (O) key;
            }
            if (String.class.equals(clazz)) {
                Object key = Long.toString(System.identityHashCode(runnable));
                BytecodeGenProxyBuilder.this.addArgument(key, runnable);
                return (O) key;
            }
            throw new AgentError("Unsupported type " + clazz.getName());
        }

        public <O> O loadLocal(final int localId, final Class<O> clazz) {
            return load(clazz, new Runnable() {
                public void run() {
                    methodAdapter.loadLocal(localId, Type.getType(clazz));
                }
            });
        }
    }
}