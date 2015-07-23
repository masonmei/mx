//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.tracers.jasper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class JasperClassFactory {
    static final Map<ClassLoader, JasperClassFactory> classFactories = new HashMap();
    private final Method visitTemplateTextMethod;
    private final Method templateTextGetTextMethod;
    private final Method templateTextSetTextMethod;
    private final Class<?> scriptletClass;
    private final Class<?> nodeClass;
    private final Class<?> markClass;
    private final Method generateVisitorVisitScriptletMethod;
    private Method visitorVisitScriptletMethod;
    private Method nodeGetParentMethod;
    private Method nodeGetQName;

    private JasperClassFactory(ClassLoader classloader) throws Exception {
        Class generateVisitorClass = classloader.loadClass("org.apache.jasper.compiler.Generator$GenerateVisitor");
        Class templateTextClass = classloader.loadClass("org.apache.jasper.compiler.Node$TemplateText");
        this.visitTemplateTextMethod = generateVisitorClass.getMethod("visit", new Class[] {templateTextClass});
        this.visitTemplateTextMethod.setAccessible(true);
        this.templateTextGetTextMethod = templateTextClass.getMethod("getText", new Class[0]);
        this.templateTextGetTextMethod.setAccessible(true);
        this.templateTextSetTextMethod = templateTextClass.getMethod("setText", new Class[] {String.class});
        this.templateTextSetTextMethod.setAccessible(true);
        this.scriptletClass = classloader.loadClass("org.apache.jasper.compiler.Node$Scriptlet");
        this.generateVisitorVisitScriptletMethod =
                generateVisitorClass.getMethod("visit", new Class[] {this.scriptletClass});
        this.generateVisitorVisitScriptletMethod.setAccessible(true);
        Class visitorClass = classloader.loadClass("org.apache.jasper.compiler.Node$Visitor");
        this.visitorVisitScriptletMethod = visitorClass.getMethod("visit", new Class[] {this.scriptletClass});
        this.visitorVisitScriptletMethod.setAccessible(true);
        this.markClass = classloader.loadClass("org.apache.jasper.compiler.Mark");
        this.nodeClass = classloader.loadClass("org.apache.jasper.compiler.Node");
        this.nodeGetParentMethod = this.nodeClass.getMethod("getParent", new Class[0]);
        this.nodeGetParentMethod.setAccessible(true);
        this.nodeGetQName = this.nodeClass.getMethod("getQName", new Class[0]);
        this.nodeGetQName.setAccessible(true);
    }

    public static synchronized JasperClassFactory getJasperClassFactory(ClassLoader cl) throws Exception {
        JasperClassFactory factory = (JasperClassFactory) classFactories.get(cl);
        if (factory == null) {
            factory = new JasperClassFactory(cl);
            classFactories.put(cl, factory);
        }

        return factory;
    }

    public Object createScriptlet(String script) throws Exception {
        return this.scriptletClass.getConstructor(new Class[] {String.class, this.markClass, this.nodeClass})
                       .newInstance(new Object[] {script, null, null});
    }

    public GenerateVisitor getGenerateVisitor(Object visitor) {
        return new JasperClassFactory.GenerateVisitorImpl(visitor);
    }

    public Node getNode(Object node) {
        return new JasperClassFactory.NodeImpl(node);
    }

    public Visitor getVisitor(Object visitor) {
        return new JasperClassFactory.VisitorImpl(visitor);
    }

    public TemplateText getTemplateText(Object text) {
        return new JasperClassFactory.TemplateTextImpl(text);
    }

    private class TemplateTextImpl implements TemplateText {
        final Object text;

        public TemplateTextImpl(Object text) {
            this.text = text;
        }

        public String getText() throws Exception {
            return (String) JasperClassFactory.this.templateTextGetTextMethod.invoke(this.text, new Object[0]);
        }

        public void setText(String text) throws Exception {
            JasperClassFactory.this.templateTextSetTextMethod.invoke(this.text, new Object[] {text});
        }
    }

    private class GenerateVisitorImpl implements GenerateVisitor {
        private final Object visitor;

        public GenerateVisitorImpl(Object visitor) {
            this.visitor = visitor;
        }

        public void visit(TemplateText text) throws Exception {
            JasperClassFactory.this.visitTemplateTextMethod
                    .invoke(this.visitor, new Object[] {((JasperClassFactory.TemplateTextImpl) text).text});
        }

        public void writeScriptlet(String script) throws Exception {
            JasperClassFactory.this.generateVisitorVisitScriptletMethod
                    .invoke(this.visitor, new Object[] {JasperClassFactory.this.createScriptlet(script)});
        }
    }

    private class VisitorImpl implements Visitor {
        private Object visitor;

        public VisitorImpl(Object visitor) {
            this.visitor = visitor;
        }

        public void writeScriptlet(String script) throws Exception {
            JasperClassFactory.this.visitorVisitScriptletMethod
                    .invoke(this.visitor, new Object[] {JasperClassFactory.this.createScriptlet(script)});
        }
    }

    private class NodeImpl implements Node {
        private final Object node;

        public NodeImpl(Object node) {
            this.node = node;
        }

        public Node getParent() throws Exception {
            return JasperClassFactory.this.new NodeImpl(JasperClassFactory.this.nodeGetParentMethod
                                                                .invoke(this.node, new Object[0]));
        }

        public String getQName() throws Exception {
            return (String) JasperClassFactory.this.nodeGetQName.invoke(this.node, new Object[0]);
        }
    }
}
