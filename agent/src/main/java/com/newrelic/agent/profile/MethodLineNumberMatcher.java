//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import com.newrelic.agent.Agent;

public class MethodLineNumberMatcher {
    public MethodLineNumberMatcher() {
    }

    public static String getMethodDescription(Class<?> currentClass, String mMethodName, int lineNumber) {
        try {
            if (currentClass != null && mMethodName != null && lineNumber > 0) {
                ClassReader e = getClassReader(currentClass);
                MethodLineNumberMatcher.LineNumberClassVisitor cv =
                        new MethodLineNumberMatcher.LineNumberClassVisitor(mMethodName, lineNumber);
                e.accept(cv, 4);
                return cv.getActualMethodDesc();
            }
        } catch (Throwable var5) {
            Agent.LOG.log(Level.FINEST, "Unable to grab method info using line numbers", var5);
        }

        return null;
    }

    private static ClassReader getClassReader(Class<?> currentClass) {
        ClassLoader loader = currentClass.getClassLoader() == null ? ClassLoader.getSystemClassLoader()
                                     : currentClass.getClassLoader();
        String resource = currentClass.getName().replace('.', '/') + ".class";
        InputStream is = null;

        ClassReader cr;
        try {
            is = loader.getResourceAsStream(resource);
            cr = new ClassReader(is);
        } catch (IOException var14) {
            throw new RuntimeException("unable to access resource: " + resource, var14);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception var13) {
                    ;
                }
            }

        }

        return cr;
    }

    public static class LineNumberClassVisitor extends ClassVisitor {
        private final String methodName;
        private final int lineNumber;
        private String actualMethodDesc;

        public LineNumberClassVisitor(ClassVisitor cv, String mName, int lNumber) {
            super(Agent.ASM_LEVEL, cv);
            this.methodName = mName;
            this.lineNumber = lNumber;
            this.actualMethodDesc = null;
        }

        public LineNumberClassVisitor(String mName, int lNumber) {
            super(Agent.ASM_LEVEL);
            this.methodName = mName;
            this.lineNumber = lNumber;
            this.actualMethodDesc = null;
        }

        public MethodVisitor visitMethod(int access, String pMethodName, final String methodDesc, String signature,
                                         String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, pMethodName, methodDesc, signature, exceptions);
            if (this.methodName.equals(pMethodName)) {
                mv = new MethodVisitor(Agent.ASM_LEVEL, mv) {
                    public void visitLineNumber(int line, Label start) {
                        super.visitLineNumber(line, start);
                        if (LineNumberClassVisitor.this.lineNumber == line) {
                            LineNumberClassVisitor.this.actualMethodDesc = methodDesc;
                        }

                    }
                };
            }

            return mv;
        }

        public boolean foundMethod() {
            return this.actualMethodDesc != null;
        }

        public String getActualMethodDesc() {
            return this.actualMethodDesc;
        }
    }
}
