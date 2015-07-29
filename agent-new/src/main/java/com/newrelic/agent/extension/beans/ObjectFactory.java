package com.newrelic.agent.extension.beans;

import javax.xml.bind.annotation.XmlRegistry;

@XmlRegistry
public class ObjectFactory {
    public Extension createExtension() {
        return new Extension();
    }

    public Extension.Instrumentation createExtensionInstrumentation() {
        return new Extension.Instrumentation();
    }

    public Extension.Instrumentation.Pointcut createExtensionInstrumentationPointcut() {
        return new Extension.Instrumentation.Pointcut();
    }

    public Extension.Instrumentation.Pointcut.Method createExtensionInstrumentationPointcutMethod() {
        return new Extension.Instrumentation.Pointcut.Method();
    }

    public Extension.Instrumentation.Pointcut.Method.Parameters
    createExtensionInstrumentationPointcutMethodParameters() {
        return new Extension.Instrumentation.Pointcut.Method.Parameters();
    }

    public Extension.Instrumentation.Pointcut.NameTransaction createExtensionInstrumentationPointcutNameTransaction() {
        return new Extension.Instrumentation.Pointcut.NameTransaction();
    }

    public Extension.Instrumentation.Pointcut.ClassName createExtensionInstrumentationPointcutClassName() {
        return new Extension.Instrumentation.Pointcut.ClassName();
    }

    public Extension.Instrumentation.Pointcut.Method.Parameters.Type
    createExtensionInstrumentationPointcutMethodParametersType() {
        return new Extension.Instrumentation.Pointcut.Method.Parameters.Type();
    }
}