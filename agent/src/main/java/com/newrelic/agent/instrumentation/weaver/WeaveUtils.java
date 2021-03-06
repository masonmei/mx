package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

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