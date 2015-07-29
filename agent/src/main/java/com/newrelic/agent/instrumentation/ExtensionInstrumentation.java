package com.newrelic.agent.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.util.InstrumentationWrapper;
import com.newrelic.deps.com.google.common.collect.Lists;

class ExtensionInstrumentation extends InstrumentationWrapper {
    private final MultiClassFileTransformer transformer = new MultiClassFileTransformer();
    private final MultiClassFileTransformer retransformingTransformer = new MultiClassFileTransformer();

    public ExtensionInstrumentation(Instrumentation delegate) {
        super(delegate);

        delegate.addTransformer(transformer);
        delegate.addTransformer(retransformingTransformer, true);
    }

    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        if (canRetransform) {
            retransformingTransformer.addTransformer(transformer);
        } else {
            this.transformer.addTransformer(transformer);
        }
    }

    public void addTransformer(ClassFileTransformer transformer) {
        this.transformer.addTransformer(transformer);
    }

    public boolean removeTransformer(ClassFileTransformer transformer) {
        return !this.transformer.removeTransformer(transformer) && retransformingTransformer
                                                                           .removeTransformer(transformer);
    }

    private static final class MultiClassFileTransformer implements ClassFileTransformer {
        private final List<ClassFileTransformer> transformers = Lists.newCopyOnWriteArrayList();

        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            byte[] originalBytes = classfileBuffer;

            for (ClassFileTransformer transformer : transformers) {
                try {
                    byte[] newBytes = transformer.transform(loader, className, classBeingRedefined, protectionDomain,
                                                                   classfileBuffer);

                    if (null != newBytes) {
                        classfileBuffer = newBytes;
                    }
                } catch (Throwable t) {
                    Agent.LOG.log(Level.FINE, "An error occurred transforming class {0} : {1}", className,
                                         t.getMessage());

                    Agent.LOG.log(Level.FINEST, t, t.getMessage());
                }

            }

            return originalBytes == classfileBuffer ? null : classfileBuffer;
        }

        public boolean removeTransformer(ClassFileTransformer transformer) {
            return transformers.remove(transformer);
        }

        public void addTransformer(ClassFileTransformer transformer) {
            transformers.add(transformer);
        }
    }
}