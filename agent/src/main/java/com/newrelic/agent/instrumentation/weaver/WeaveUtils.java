package com.newrelic.agent.instrumentation.weaver;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

public class WeaveUtils {
    public static final String NEW_FIELD_ANNOTATION_DESCRIPTOR = Type.getDescriptor(NewField.class);

    static final Method CALL_ORIGINAL_METHOD = new Method("callOriginal", Type.getType(Object.class), new Type[0]);

    public static boolean isWeavedClass(ClassReader reader) {
        final boolean[] weaved = {false};
        reader.accept(new ClassVisitor(Agent.ASM_LEVEL) {
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (Type.getDescriptor(Weave.class).equals(desc)) {
                    weaved[0] = true;
                }
                return null;
            }
        }, 1);

        return weaved[0];
    }
}