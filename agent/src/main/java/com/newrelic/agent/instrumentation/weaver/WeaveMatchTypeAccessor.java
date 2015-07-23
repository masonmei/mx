package com.newrelic.agent.instrumentation.weaver;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import com.newrelic.agent.Agent;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

public class WeaveMatchTypeAccessor {
    private MatchType matchType;

    public MatchType getMatchType() {
        return matchType;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible, AnnotationVisitor va) {
        if (desc.equals(Type.getType(Weave.class).getDescriptor())) {
            matchType = MatchType.ExactClass;
            return new AnnotationVisitor(Agent.ASM_LEVEL, va) {
                public void visitEnum(String name, String desc, String value) {
                    matchType = MatchType.valueOf(value);
                    super.visitEnum(name, desc, value);
                }
            };
        }
        return va;
    }
}