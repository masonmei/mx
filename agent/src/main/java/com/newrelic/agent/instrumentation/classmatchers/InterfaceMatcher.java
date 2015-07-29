package com.newrelic.agent.instrumentation.classmatchers;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.BenignClassReadException;
import com.newrelic.agent.util.asm.MissingResourceException;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.Type;

public class InterfaceMatcher extends ClassMatcher {
    private final Type type;
    private final String internalName;

    public InterfaceMatcher(String interfaceName) {
        type = Type.getObjectType(Strings.fixInternalClassName(interfaceName));
        internalName = type.getInternalName();
    }

    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        if ((cr.getAccess() & 0x200) != 0) {
            return false;
        }

        if (Utils.getClassResource(loader, type) == null) {
            return false;
        }

        String[] interfaces = cr.getInterfaces();
        if (isInterfaceMatch(loader, interfaces)) {
            return true;
        }

        String superName = cr.getSuperName();
        if ((superName != null) && (!superName.equals("java/lang/Object"))) {
            try {
                cr = Utils.readClass(loader, superName);
                return isMatch(loader, cr);
            } catch (MissingResourceException e) {
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG
                            .finest(MessageFormat.format("Unable to load class {0}: {1}", new Object[] {superName, e}));
                }
            } catch (BenignClassReadException ex) {
            } catch (IOException ex) {
                Agent.LOG.log(Level.FINEST, "Unable to match " + internalName, ex);
            }
        }

        return false;
    }

    private boolean isInterfaceMatch(ClassLoader loader, String[] interfaces) {
        if (isNameMatch(interfaces)) {
            return true;
        }

        for (String interfaceName : interfaces) {
            if (isInterfaceMatch(loader, interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNameMatch(String[] interfaces) {
        for (String interfaceName : interfaces) {
            if (internalName.equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInterfaceMatch(ClassLoader loader, String interfaceName) {
        try {
            ClassReader reader = Utils.readClass(loader, interfaceName);
            return isInterfaceMatch(loader, reader.getInterfaces());
        } catch (MissingResourceException e) {
            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.finest(MessageFormat
                                         .format("Unable to load interface {0}: {1}", new Object[] {interfaceName, e}));
            }
            return false;
        } catch (BenignClassReadException e) {
            return false;
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to load interface {0}: {1}", new Object[] {interfaceName, e});
            if (Agent.LOG.isFinestEnabled()) {
                if (interfaceName.startsWith("com/newrelic/agent/")) {
                    Agent.LOG.log(Level.FINEST, msg);
                } else {
                    Agent.LOG.log(Level.FINEST, msg, e);
                }
            } else {
                Agent.LOG.finer(msg);
            }
        }
        return false;
    }

    public boolean isMatch(Class<?> clazz) {
        try {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader == null) {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            if (Utils.getClassResource(classLoader, type) == null) {
                return false;
            }
            Class interfaceClass = classLoader.loadClass(type.getClassName());
            if ((interfaceClass.isInterface()) && (interfaceClass.isAssignableFrom(clazz))) {
                return true;
            }
        } catch (Exception ex) {
        } catch (Error e) {
        }
        return false;
    }

    public String toString() {
        return internalName;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (type == null ? 0 : type.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InterfaceMatcher other = (InterfaceMatcher) obj;
        if (type == null) {
            return other.type == null;
        }
        return type.equals(other.type);
    }

    public Collection<String> getClassNames() {
        return Arrays.asList(new String[] {internalName});
    }
}