package com.newrelic.agent.instrumentation;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

public abstract class AbstractClassTransformer implements StartableClassFileTransformer {
    private final int classreaderFlags;
    private final boolean enabled;

    public AbstractClassTransformer(int classreaderFlags) {
        this(classreaderFlags, true);
    }

    public AbstractClassTransformer(int classreaderFlags, boolean enabled) {
        this.enabled = enabled;
        this.classreaderFlags = classreaderFlags;
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        try {
            if (!matches(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)) {
                return null;
            }
            if (!isAbleToResolveAgent(loader, className)) {
                String msg = MessageFormat.format("Not instrumenting {0}: class loader unable to load agent classes",
                                                         new Object[] {className});

                Agent.LOG.log(Level.FINER, msg);
                return null;
            }
            byte[] classBytesWithUID = InstrumentationUtils.generateClassBytesWithSerialVersionUID(classfileBuffer,
                                                                                                          classreaderFlags,
                                                                                                          loader);

            ClassReader cr = new ClassReader(classBytesWithUID);
            ClassWriter cw = InstrumentationUtils.getClassWriter(cr, loader);
            ClassVisitor classVisitor = getClassVisitor(cr, cw, className, loader);
            if (null == classVisitor) {
                return null;
            }
            cr.accept(classVisitor, classreaderFlags);
            String msg = MessageFormat.format("Instrumenting {0}", new Object[] {className});
            Agent.LOG.finer(msg);

            return cw.toByteArray();
        } catch (StopProcessingException e) {
            String msg = MessageFormat.format("Instrumentation aborted for {0}: {1}", new Object[] {className, e});
            Agent.LOG.log(Level.FINER, msg, e);
            return null;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Instrumentation error for {0}: {1}", new Object[] {className, t});
            Agent.LOG.log(Level.FINER, msg, t);
        }
        return null;
    }

    protected boolean isAbleToResolveAgent(ClassLoader loader, String className) {
        return InstrumentationUtils.isAbleToResolveAgent(loader, className);
    }

    protected int getClassReaderFlags() {
        return classreaderFlags;
    }

    public void start(InstrumentationProxy instrumentation, boolean isRetransformSupported) {
        boolean canRetransform = (isRetransformSupported) && (isRetransformSupported());
        if (isEnabled()) {
            instrumentation.addTransformer(this, canRetransform);
            start();
        }
    }

    protected void start() {
    }

    protected boolean isEnabled() {
        return enabled;
    }

    protected abstract boolean isRetransformSupported();

    protected abstract ClassVisitor getClassVisitor(ClassReader paramClassReader, ClassWriter paramClassWriter,
                                                    String paramString, ClassLoader paramClassLoader);

    protected abstract boolean matches(ClassLoader paramClassLoader, String paramString, Class<?> paramClass,
                                       ProtectionDomain paramProtectionDomain, byte[] paramArrayOfByte);
}