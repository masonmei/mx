package com.newrelic.agent.instrumentation;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;
import com.newrelic.deps.org.objectweb.asm.Type;

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
        this.logger = Agent.LOG.getChildLogger(ClassTransformer.class);
        this.classMatcher = classMatcher;
        this.skipClassMatcher = skipMatcher;
        this.interfaceToImplement = interfaceToImplement;
        this.originalInterface = originalInterfaceName;
        this.originalInterfaceType = Type.getObjectType(this.originalInterface);
        this.enabled = enabled;

        this.classTransformer = classTransformer;
        this.classreaderFlags = classTransformer.getClassReaderFlags();
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
        if (this.enabled) {
            instrumentation.addTransformer(this, false);
        }
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        boolean isLoggable = false;
        if (classBeingRedefined != null) {
            return null;
        }
        if ((loader == null) && (Agent.class.getClassLoader() != null)) {
            return null;
        }
        ClassReader cr = new ClassReader(classfileBuffer);
        if (InstrumentationUtils.isInterface(cr)) {
            return null;
        }
        if (this.skipClassMatcher.isMatch(loader, cr)) {
            return null;
        }
        boolean matches = this.classMatcher.isMatch(loader, cr);
        try {
            if (!matches) {
                if (!isGenericInterfaceSupportEnabled()) {
                    return null;
                }
                if (excludeClass(className)) {
                    return null;
                }

                if (!matches(cr, this.originalInterface)) {
                    return null;
                }
            }

            if (!InstrumentationUtils.isAbleToResolveAgent(loader, className)) {
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat
                                         .format("Not instrumenting {0}: class loader unable to load agent classes",
                                                        new Object[] {className});

                    Agent.LOG.log(Level.FINER, msg);
                }
                return null;
            }
            if ("org/eclipse/jetty/server/Request".equals(className)) {
                className.hashCode();
            }
            byte[] classBytesWithUID =
                    InstrumentationUtils.generateClassBytesWithSerialVersionUID(cr, this.classreaderFlags, loader);

            ClassReader crWithUID = new ClassReader(classBytesWithUID);
            ClassWriter cwWithUID = InstrumentationUtils.getClassWriter(crWithUID, loader);
            cr.accept(createClassVisitor(crWithUID, cwWithUID, className, loader), this.classreaderFlags);

            return cwWithUID.toByteArray();
        } catch (StopProcessingException e) {
            String msg = MessageFormat.format("Instrumentation aborted for {0} - {1} ", new Object[] {className, e});
            Agent.LOG.finer(msg);
            return null;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Instrumentation error for {0} - {1} ", new Object[] {className, t});
            Agent.LOG.finer(msg);
        }
        return null;
    }

    protected boolean excludeClass(String className) {
        return this.classTransformer.isExcluded(className);
    }

    protected boolean isGenericInterfaceSupportEnabled() {
        return true;
    }

    protected int getClassReaderFlags() {
        return this.classreaderFlags;
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