package com.newrelic.agent;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.newrelic.deps.com.google.common.collect.Lists;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.BootstrapLoader;
import com.newrelic.agent.util.InstrumentationWrapper;
import com.newrelic.agent.util.Streams;
import com.newrelic.agent.util.asm.Utils;

public class InstrumentationProxy extends InstrumentationWrapper {
    private final boolean bootstrapClassIntrumentationEnabled;

    protected InstrumentationProxy(Instrumentation instrumentation,
                                   boolean enableBootstrapClassInstrumentationDefault) {
        super(instrumentation);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        bootstrapClassIntrumentationEnabled = ((Boolean) config.getProperty("enable_bootstrap_class_instrumentation",
                                                                                   Boolean.valueOf(enableBootstrapClassInstrumentationDefault)))
                                                      .booleanValue();
    }

    public static InstrumentationProxy getInstrumentationProxy(Instrumentation inst) {
        if (inst == null) {
            return null;
        }
        return new InstrumentationProxy(inst, true);
    }

    public static void forceRedefinition(Instrumentation instrumentation, Class<?>[] classes)
            throws ClassNotFoundException, UnmodifiableClassException {
        List toRedefine = Lists.newArrayList();
        for (Class clazz : classes) {
            String classResourceName = Utils.getClassResourceName(clazz);
            URL resource = clazz.getResource(classResourceName);
            if (resource == null) {
                resource = BootstrapLoader.get().getBootstrapResource(classResourceName);
            }
            if (resource != null) {
                try {
                    byte[] classfileBuffer = Streams.read(resource.openStream(), true);

                    toRedefine.add(new ClassDefinition(clazz, classfileBuffer));
                } catch (Exception e) {
                    Agent.LOG.finer("Unable to redefine " + clazz.getName() + " - " + e.toString());
                }
            } else {
                Agent.LOG.finer("Unable to find resource to redefine " + clazz.getName());
            }
        }

        if (!toRedefine.isEmpty()) {
            instrumentation.redefineClasses((ClassDefinition[]) toRedefine.toArray(new ClassDefinition[0]));
        }
    }

    protected Instrumentation getInstrumentation() {
        return delegate;
    }

    public void redefineClasses(ClassDefinition[] definitions)
            throws ClassNotFoundException, UnmodifiableClassException {
        if (isRedefineClassesSupported()) {
            super.redefineClasses(definitions);
        }
    }

    public Class<?>[] retransformUninstrumentedClasses(String[] classNames)
            throws UnmodifiableClassException, ClassNotFoundException {
        if (!isRetransformClassesSupported()) {
            return new Class[0];
        }

        List classList = new ArrayList(classNames.length);
        for (String className : classNames) {
            Class clazz = Class.forName(className);
            if (!ClassTransformer.isInstrumented(clazz)) {
                classList.add(clazz);
            }
        }

        Class[] classArray = (Class[]) classList.toArray(new Class[0]);
        if (!classList.isEmpty()) {
            retransformClasses(classArray);
        }

        return classArray;
    }

    public int getClassReaderFlags() {
        return 8;
    }

    public final boolean isBootstrapClassInstrumentationEnabled() {
        return bootstrapClassIntrumentationEnabled;
    }

    public boolean isAppendToClassLoaderSearchSupported() {
        return true;
    }
}