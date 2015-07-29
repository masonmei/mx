package com.newrelic.agent.instrumentation.weaver;

import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.Logger;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.AdviceAdapter;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class LogApiCallsVisitor extends ClassVisitor {
    private final InstrumentationPackage instrumentationPackage;
    private final Type[] apiClassesTypes;
    private String className;

    public LogApiCallsVisitor(InstrumentationPackage instrumentationPackage, ClassVisitor cv) {
        super(327680, cv);
        this.instrumentationPackage = instrumentationPackage;

        this.apiClassesTypes = new Type[AgentBridge.API_CLASSES.length];
        for (int i = 0; i < AgentBridge.API_CLASSES.length; i++) {
            this.apiClassesTypes[i] = Type.getType(AgentBridge.API_CLASSES[i]);
        }
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    public MethodVisitor visitMethod(final int access, String name, String desc, String signature,
                                     String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        final Method method = new Method(name, desc);
        return new AdviceAdapter(327680, mv, access, name, desc) {
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                logApiCall(method, access, owner, name, desc, LogApiCallsVisitor.this.apiClassesTypes);
            }

            private boolean logApiCall(Method instrumentationMethod, int access, String owner, String name, String desc,
                                       Type[] apiTypes) {
                for (Type apiType : apiTypes) {
                    if (apiType.getInternalName().equals(owner)) {
                        String logMessage = Strings.join(new String[] {LogApiCallsVisitor.this.className, ".",
                                                                              instrumentationMethod.toString(),
                                                                              " called ", apiType.getClassName(), ".",
                                                                              new Method(name, desc).toString()});

                        Logger logger = BridgeUtils.getLogger(this);

                        logger.logToChild(LogApiCallsVisitor.this.instrumentationPackage.getImplementationTitle(),
                                                 Level.FINEST, logMessage, (Object[]) null);

                        return true;
                    }
                }
                return false;
            }
        };
    }
}