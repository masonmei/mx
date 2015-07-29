package com.newrelic.agent.instrumentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.FieldVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;
import com.newrelic.deps.org.objectweb.asm.commons.GeneratorAdapter;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.deps.org.objectweb.asm.commons.Method;

public class FieldAccessorGeneratingClassAdapter extends ClassVisitor {
    private final String className;
    private final Map<String, String> allFields = new HashMap();
    private final java.lang.reflect.Method[] methods;
    private final boolean hasFieldAccessors;

    public FieldAccessorGeneratingClassAdapter(ClassVisitor cv, String className, Class<?> extensionClass) {
        super(Agent.ASM_LEVEL, cv);
        this.className = className;
        methods = extensionClass.getMethods();
        hasFieldAccessors = hasFieldAccessors();
    }

    private boolean hasFieldAccessors() {
        for (java.lang.reflect.Method method : methods) {
            if (method.getAnnotation(FieldAccessor.class) != null) {
                return true;
            }
        }
        return false;
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (hasFieldAccessors) {
            allFields.put(name, desc);
        }
        return super.visitField(access, name, desc, signature, value);
    }

    public void visitEnd() {
        if (hasFieldAccessors) {
            addFieldAccessors();
        }
        super.visitEnd();
    }

    private void addFieldAccessors() {
        Map<String, Object[]> fields = new HashMap<String, Object[]>();
        for (java.lang.reflect.Method method : methods) {
            FieldAccessor fieldAccessor = method.getAnnotation(FieldAccessor.class);
            if (fieldAccessor != null) {
                Class returnType = method.getReturnType();
                if (Void.TYPE.equals(returnType)) {
                    Class[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 1) {
                        returnType = parameterTypes[0];
                    }
                }
                String fieldName = (fieldAccessor.existingField() ? "" : "__nr__") + fieldAccessor.fieldName();
                Object[] fieldDesc = (Object[]) fields.get(fieldName);
                Class type = fieldDesc == null ? null : (Class) fieldDesc[0];
                if ((type != null) && (!returnType.equals(type))) {
                    throw new StopProcessingException("Method " + method.getName() + " uses type " + type.getName()
                                                              + ", but " + returnType.getName() + " was expected.");
                }

                if (!fieldAccessor.existingField()) {
                    fieldDesc = new Object[2];
                    fieldDesc[0] = returnType;
                    fieldDesc[1] = Integer.valueOf(fieldAccessor.volatileAccess() ? 194 : 130);

                    fields.put(fieldName, fieldDesc);
                } else if (!allFields.containsKey(fieldName)) {
                    throw new StopProcessingException(className + " does not contain a field named " + fieldName);
                }

                Type returnType2 = Type.getType(returnType);
                Type fieldType = returnType2;
                if (fieldAccessor.fieldDesc().length() > 0) {
                    fieldType = Type.getType(fieldAccessor.fieldDesc());
                }
                writeMethod(fieldName, returnType2, fieldType, method);
            }
        }

        for (Entry<String, Object[]> field : fields.entrySet()) {
            String fieldName = field.getKey();
            Object[] fieldDesc = field.getValue();
            Type fieldType = Type.getType((Class) fieldDesc[0]);
            cv.visitField(((Integer) fieldDesc[1]).intValue(), fieldName, fieldType.getDescriptor(), null, null);
        }
    }

    private void writeMethod(String fieldName, Type returnType, Type fieldType, java.lang.reflect.Method method) {
        boolean setter = Void.TYPE.equals(method.getReturnType());
        Method newMethod = InstrumentationUtils.getMethod(method);
        GeneratorAdapter mv = new GeneratorAdapter(1, newMethod, null, null, this);
        mv.visitCode();

        mv.loadThis();
        if (setter) {
            mv.loadArgs();
        }
        int op = setter ? 181 : 180;
        mv.visitFieldInsn(op, className, fieldName, fieldType.getDescriptor());

        op = setter ? 177 : returnType.getOpcode(172);
        mv.visitInsn(op);
        mv.visitMaxs(0, 0);
    }
}