package com.newrelic.agent.instrumentation;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;
import com.newrelic.deps.org.objectweb.asm.Type;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.logging.IAgentLogger;

public abstract class AbstractImplementationClassTransformer implements StartableClassFileTransformer {
    protected final IAgentLogger logger;
    protected final int classreaderFlags;
    protected final ClassTransformer classTransformer;
    protected final Class interfaceToImplement;
    protected final String originalInterface;
    protected final Type originalInterfaceType;
    private final ClassMatcher classMatcher;
    private final boolean enabled;
    private final ClassMatcher skipClassMatcher;

    public AbstractImplementationClassTransformer(ClassTransformer classTransformer, boolean enabled,
                                                  Class interfaceToImplement, ClassMatcher classMatcher,
                                                  ClassMatcher skipMatcher, String originalInterfaceName) {
        logger = Agent.LOG.getChildLogger(ClassTransformer.class);
        this.classMatcher = classMatcher;
        skipClassMatcher = skipMatcher;
        this.interfaceToImplement = interfaceToImplement;
        originalInterface = originalInterfaceName;
        originalInterfaceType = Type.getObjectType(originalInterface);
        this.enabled = enabled;

        this.classTransformer = classTransformer;
        classreaderFlags = classTransformer.getClassReaderFlags();
    }

    public AbstractImplementationClassTransformer(ClassTransformer classTransformer, boolean enabled,
                                                  Class interfaceToImplement) {
        this(classTransformer, enabled, interfaceToImplement, getClassMatcher(interfaceToImplement),
                    getSkipClassMatcher(interfaceToImplement), getOriginalInterface(interfaceToImplement));
    }

    private static String getOriginalInterface(Class interfaceToImplement) {
        InterfaceMapper interfaceMapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        return interfaceMapper.originalInterfaceName();
    }

    private static ClassMatcher getClassMatcher(Class interfaceToImplement) {
        InterfaceMapper interfaceMapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        if (interfaceMapper.className().length == 0) {
            return new ExactClassMatcher(interfaceMapper.originalInterfaceName());
        }
        return ExactClassMatcher.or(interfaceMapper.className());
    }

    private static ClassMatcher getSkipClassMatcher(Class interfaceToImplement) {
        InterfaceMapper interfaceMapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        return ExactClassMatcher.or(interfaceMapper.skip());
    }

    public void start(InstrumentationProxy instrumentation, boolean isRetransformSupported) {
        if (enabled) {
            instrumentation.addTransformer(this, false);
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        boolean isLoggable = false;
        if (classBeingRedefined != null) {
            return null;
        } else if (loader == null && Agent.class.getClassLoader() != null) {
            return null;
        } else {
            ClassReader cr = new ClassReader(classfileBuffer);
            if (InstrumentationUtils.isInterface(cr)) {
                return null;
            } else if (this.skipClassMatcher.isMatch(loader, cr)) {
                return null;
            } else {
                boolean matches = this.classMatcher.isMatch(loader, cr);

                String msg;
                try {
                    if (!matches) {
                        if (!this.isGenericInterfaceSupportEnabled()) {
                            return null;
                        }

                        if (this.excludeClass(className)) {
                            return null;
                        }

                        if (!this.matches(cr, this.originalInterface)) {
                            return null;
                        }
                    }

                    if (!InstrumentationUtils.isAbleToResolveAgent(loader, className)) {
                        if (Agent.LOG.isLoggable(Level.FINER)) {
                            String t1 = MessageFormat.format("Not instrumenting {0}: class loader unable to load agent "
                                                                     + "classes", className);
                            Agent.LOG.log(Level.FINER, t1);
                        }

                        return null;
                    } else {
                        if ("org/eclipse/jetty/server/Request".equals(className)) {
                            className.hashCode();
                        }

                        byte[] t = InstrumentationUtils
                                           .generateClassBytesWithSerialVersionUID(cr, this.classreaderFlags, loader);
                        ClassReader msg1 = new ClassReader(t);
                        ClassWriter cwWithUID = InstrumentationUtils.getClassWriter(msg1, loader);
                        cr.accept(this.createClassVisitor(msg1, cwWithUID, className, loader), this.classreaderFlags);
                        return cwWithUID.toByteArray();
                    }
                } catch (StopProcessingException var12) {
                    msg = MessageFormat.format("Instrumentation aborted for {0} - {1} ", className, var12);
                    Agent.LOG.finer(msg);
                    return null;
                } catch (Throwable var13) {
                    msg = MessageFormat.format("Instrumentation error for {0} - {1} ", className, var13);
                    Agent.LOG.finer(msg);
                    return null;
                }
            }
        }
    }

    protected boolean excludeClass(String className) {
        return classTransformer.isExcluded(className);
    }

    protected boolean isGenericInterfaceSupportEnabled() {
        return true;
    }

    protected int getClassReaderFlags() {
        return classreaderFlags;
    }

    protected abstract ClassVisitor createClassVisitor(ClassReader paramClassReader, ClassWriter paramClassWriter,
                                                       String paramString, ClassLoader paramClassLoader);

    private boolean matches(ClassReader cr, String interfaceNameToMatch) {
        String[] interfaces = cr.getInterfaces();
        if (interfaces != null) {
            for (String interfaceName : interfaces) {
                if (interfaceNameToMatch.equals(interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }
}