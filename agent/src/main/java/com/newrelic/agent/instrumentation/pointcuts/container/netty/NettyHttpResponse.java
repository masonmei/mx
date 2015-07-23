//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import java.util.List;
import java.util.Map.Entry;

import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;

@InterfaceMixin(
                       originalClassName = {"org/jboss/netty/handler/codec/http/DefaultHttpResponse"})
public interface NettyHttpResponse {
    @FieldAccessor(
                          fieldName = "status",
                          fieldDesc = "Lorg/jboss/netty/handler/codec/http/HttpResponseStatus;",
                          existingField = true)
    NettyHttpResponse.HttpResponseStatus _nr_status();

    List<String> getHeaders(String var1);

    List<Entry<String, String>> getHeaders();

    void setHeader(String var1, Object var2);

    @InterfaceMixin(
                           originalClassName = {"org/jboss/netty/handler/codec/http/HttpResponseStatus"})
    public interface HttpResponseStatus {
        String CLASS = "org/jboss/netty/handler/codec/http/HttpResponseStatus";

        int getCode();

        String getReasonPhrase();
    }
}
