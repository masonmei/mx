package com.newrelic.agent.instrumentation.pointcuts.database;

import com.newrelic.agent.instrumentation.AbstractImplementationClassTransformer;
import com.newrelic.agent.instrumentation.AddInterfaceAdapter;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.FieldAccessorGeneratingClassAdapter;
import com.newrelic.agent.instrumentation.RequireMethodsAdapter;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.NoMatchMatcher;
import com.newrelic.deps.org.objectweb.asm.ClassReader;
import com.newrelic.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.deps.org.objectweb.asm.ClassWriter;

public class ConnectionClassTransformer extends AbstractImplementationClassTransformer {
    public ConnectionClassTransformer(ClassTransformer classTransformer) {
        super(classTransformer, true, ConnectionExtension.class, ExactClassMatcher
                                                                         .or(new String[]
                                                                                     {"com/mysql/jdbc/ConnectionImpl",
                                                                                                  "oracle/jdbc/driver/PhysicalConnection",
                                                                                                  "oracle/jdbc/OracleConnectionWrapper",
                                                                                                  "org/apache/derby/impl/jdbc/EmbedConnection"}),
                     NoMatchMatcher.MATCHER, "java/sql/Connection");
    }

    protected ClassVisitor createClassVisitor(ClassReader cr, ClassWriter cw, String className, ClassLoader loader) {
        ClassVisitor adapter = new AddInterfaceAdapter(cw, className, ConnectionExtension.class);
        adapter = RequireMethodsAdapter.getRequireMethodsAdaptor(adapter, className, ConnectionExtension.class, loader);
        adapter = new FieldAccessorGeneratingClassAdapter(adapter, className, ConnectionExtension.class);
        return adapter;
    }
}