//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.weaver;

import java.util.logging.Level;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.util.Strings;

public class LogWeavedMethodInvocationsVisitor extends ClassVisitor {
    private final InstrumentationPackage instrumentationPackage;
    private String className;

    public LogWeavedMethodInvocationsVisitor(InstrumentationPackage instrumentationPackage, ClassVisitor cv) {
        super(Agent.ASM_LEVEL, cv);
        this.instrumentationPackage = instrumentationPackage;
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature,
                                     String[] exceptions) {
        final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        return (MethodVisitor) ((access & 1024) != 0 ? mv : ("<init>".equals(name)
                                                                     ? new LogWeavedMethodInvocationsVisitor
                                                                                   .BaseLogAdapter(Agent.ASM_LEVEL,
                                                                                                                                   mv,
                                                                                                                                   access,
                                                                                                                                   name,
                                                                                                                                   desc) {
            protected void onMethodExit(int opcode) {
                this.logMethod();
            }
        } : new LogWeavedMethodInvocationsVisitor.BaseLogAdapter(Agent.ASM_LEVEL, mv, access, name, desc) {
            protected void onMethodEnter() {
                this.logMethod();
            }
        }));
    }

    private class BaseLogAdapter extends AdviceAdapter {
        private final Method method;

        protected BaseLogAdapter(int api, MethodVisitor mv, int access, String name, String desc) {
            super(api, mv, access, name, desc);
            this.method = new Method(name, desc);
        }

        protected void logMethod() {
            String message = Strings.join(new String[] {LogWeavedMethodInvocationsVisitor.this.className, ".",
                                                               this.method.toString(), " invoked"});
            BridgeUtils.getLogger(this)
                    .logToChild(LogWeavedMethodInvocationsVisitor.this.instrumentationPackage.getImplementationTitle(),
                                       Level.FINEST, message, (Object[]) null);
        }
    }
}
