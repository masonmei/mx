package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.deps.org.objectweb.asm.Type;

public class WeaveMatchTypeAccessor {
    private MatchType matchType;

    public MatchType getMatchType() {
        return this.matchType;
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible, AnnotationVisitor va) {
        if (desc.equals(Type.getType(Weave.class).getDescriptor())) {
            this.matchType = MatchType.ExactClass;
            return new AnnotationVisitor(327680, va) {
                public void visitEnum(String name, String desc, String value) {
                    WeaveMatchTypeAccessor.this.matchType = MatchType.valueOf(value);
                    super.visitEnum(name, desc, value);
                }
            };
        }
        return va;
    }
}