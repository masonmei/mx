//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.play2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.FieldAccessor;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.instrumentation.pointcuts.container.netty.DelegatingNettyHttpRequest;
import com.newrelic.agent.instrumentation.pointcuts.scala.ScalaCollectionJavaConversions;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;

@PointCut
public class Play2RequestPointCut extends com.newrelic.agent.instrumentation.PointCut implements
        EntryInvocationHandler {
    static final String PLAY21CLASS = "play/api/mvc/Request$";
    static final String REQUEST_METHOD_NAME = "apply";
    static final String REQUEST_METHOD_DESC = "(Lplay/api/mvc/RequestHeader;Ljava/lang/Object;)Lplay/api/mvc/Request;";
    static final String PLAY20CLASS = "play/core/server/Server$class";
    static final String SERVER_METHOD_NAME = "invoke";
    static final String SERVER_METHOD_DESC =
            "(Lplay/core/server/Server;Lplay/api/mvc/Request;Lplay/api/mvc/Response;Lplay/api/mvc/Action;"
                    + "Lplay/api/Application;)V";
    private static final boolean DEFAULT_ENABLED = true;
    private static final String POINT_CUT_NAME = Play2RequestPointCut.class.getName();

    public Play2RequestPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration(POINT_CUT_NAME, "play2_instrumentation", true);
    }

    private static ClassMatcher createClassMatcher() {
        return OrClassMatcher.getClassMatcher(new ClassMatcher[] {new ExactClassMatcher("play/api/mvc/Request$"),
                                                                         new ExactClassMatcher
                                                                                 ("play/core/server/Server$class")});
    }

    private static MethodMatcher createMethodMatcher() {
        return OrMethodMatcher.getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("apply",
                                                                                                   "(Lplay/api/mvc/RequestHeader;Ljava/lang/Object;)Lplay/api/mvc/Request;"),
                                                                            new ExactMethodMatcher("invoke",
                                                                                                          "(Lplay/core/server/Server;Lplay/api/mvc/Request;Lplay/api/mvc/Response;Lplay/api/mvc/Action;Lplay/api/Application;)V")});
    }

    protected PointCutInvocationHandler getPointCutInvocationHandlerImpl() {
        return this;
    }

    public void handleInvocation(ClassMethodSignature sig, Object object, Object[] args) {
        Transaction tx = Transaction.getTransaction();
        if (tx.isStarted()) {
            com.newrelic.api.agent.Request request = tx.getRootTransaction().getDispatcher().getRequest();
            HashMap extractedForm = new HashMap();
            if (request instanceof DelegatingNettyHttpRequest) {
                DelegatingNettyHttpRequest nettyRequest = (DelegatingNettyHttpRequest) request;
                Object bodyCandidate = args[1];
                Object data;
                Map formValues;
                Iterator i$;
                Entry formValue;
                List values;
                if (args[1] instanceof Play2RequestPointCut.Request) {
                    Play2RequestPointCut.Request formBody1 = (Play2RequestPointCut.Request) args[1];
                    bodyCandidate = formBody1.nr_body();
                    data = formBody1.nr_queryString();
                    if (data != null) {
                        formValues = ScalaCollectionJavaConversions.asJavaMap(data);
                        i$ = formValues.entrySet().iterator();

                        while (i$.hasNext()) {
                            formValue = (Entry) i$.next();
                            values = ScalaCollectionJavaConversions.asJavaList(formValue.getValue());
                            extractedForm.put((String) formValue.getKey(), new ArrayList(values));
                        }
                    }
                } else if (args[0] instanceof Play2RequestPointCut.RequestHeader) {
                    Play2RequestPointCut.RequestHeader formBody = (Play2RequestPointCut.RequestHeader) args[0];
                    data = formBody.nr_queryString();
                    if (data != null) {
                        formValues = ScalaCollectionJavaConversions.asJavaMap(data);
                        i$ = formValues.entrySet().iterator();

                        while (i$.hasNext()) {
                            formValue = (Entry) i$.next();
                            values = ScalaCollectionJavaConversions.asJavaList(formValue.getValue());
                            extractedForm.put((String) formValue.getKey(), new ArrayList(values));
                        }
                    }
                }

                if (bodyCandidate instanceof Play2RequestPointCut.AnyContentAsFormUrlEncoded) {
                    Play2RequestPointCut.AnyContentAsFormUrlEncoded formBody2 =
                            (Play2RequestPointCut.AnyContentAsFormUrlEncoded) bodyCandidate;
                    data = formBody2.nr_data();
                    if (data != null) {
                        formValues = ScalaCollectionJavaConversions.asJavaMap(data);
                        i$ = formValues.entrySet().iterator();

                        while (i$.hasNext()) {
                            formValue = (Entry) i$.next();
                            values = ScalaCollectionJavaConversions.asJavaList(formValue.getValue());
                            if (extractedForm.containsKey(formValue.getKey())) {
                                ((List) extractedForm.get(formValue.getKey())).addAll(values);
                            } else {
                                extractedForm.put((String) formValue.getKey(), new ArrayList(values));
                            }
                        }
                    }
                }

                nettyRequest.setParameters(extractedForm);
            }

        }
    }

    @InterfaceMixin(
                           originalClassName = {"play/api/mvc/AnyContentAsFormUrlEncoded"})
    public interface AnyContentAsFormUrlEncoded {
        String CLASS = "play/api/mvc/AnyContentAsFormUrlEncoded";

        @FieldAccessor(
                              fieldName = "data",
                              existingField = true,
                              fieldDesc = "Lscala/collection/immutable/Map;")
        Object nr_data();
    }

    @InterfaceMapper(
                            originalInterfaceName = "play/api/mvc/RequestHeader",
                            className = {"play/api/mvc/RequestHeader$$anon$4"})
    public interface RequestHeader {
        String INTERFACE = "play/api/mvc/RequestHeader";
        String CLASS = "play/api/mvc/RequestHeader$$anon$4";

        @MethodMapper(
                             originalMethodName = "queryString",
                             originalDescriptor = "()Lscala/collection/immutable/Map;")
        Object nr_queryString();
    }

    @InterfaceMapper(
                            originalInterfaceName = "play/api/mvc/Request",
                            className =
                                    {"play/core/server/netty/PlayDefaultUpstreamHandler$$anonfun$19$$anonfun$apply$21$$anon$1"})
    public interface Request {
        String INTERFACE = "play/api/mvc/Request";
        String CLASS = "play/core/server/netty/PlayDefaultUpstreamHandler$$anonfun$19$$anonfun$apply$21$$anon$1";

        @MethodMapper(
                             originalMethodName = "body")
        Object nr_body();

        @MethodMapper(
                             originalMethodName = "queryString",
                             originalDescriptor = "()Lscala/collection/immutable/Map;")
        Object nr_queryString();
    }
}
