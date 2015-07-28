package com.newrelic.agent.instrumentation.pointcuts.commons;

import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;

@InterfaceMapper(originalInterfaceName = "org/apache/commons/httpclient/HttpMethod",
                        className = {"org/apache/commons/httpclient/HttpMethodBase"})
public interface HttpMethodExtension {
    @MethodMapper(originalMethodName = "getRequestHeader",
                         originalDescriptor = "(Ljava/lang/String;)Lorg/apache/commons/httpclient/Header;",
                         invokeInterface = false)
    Object _nr_getRequestHeader(String paramString);

    @MethodMapper(originalMethodName = "getResponseHeader",
                         originalDescriptor = "(Ljava/lang/String;)Lorg/apache/commons/httpclient/Header;",
                         invokeInterface = false)
    Object _nr_getResponseHeader(String paramString);

    @MethodMapper(originalMethodName = "getURI",
                         originalDescriptor = "()Lorg/apache/commons/httpclient/URI;",
                         invokeInterface = false)
    Object _nr_getUri();
}