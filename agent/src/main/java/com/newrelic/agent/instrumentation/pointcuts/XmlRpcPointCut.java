//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.InstrumentUtils;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.IOTracer;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class XmlRpcPointCut extends TracerFactoryPointCut {
    public XmlRpcPointCut(ClassTransformer classTransformer) {
        super(XmlRpcPointCut.class, new InterfaceMatcher("javax/xml/rpc/Call"), createExactMethodMatcher("invoke",
                                                                                                                new String[] {"([Ljava/lang/Object;)Ljava/lang/Object;",
                                                                                                                                     "(Ljavax/xml/namespace/QName;[Ljava/lang/Object;)Ljava/lang/Object;"}));
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object call, Object[] args) {
        try {
            String e = (String) call.getClass().getMethod("getTargetEndpointAddress", new Class[0])
                                        .invoke(call, new Object[0]);

            try {
                URL e1 = new URL(e);
                String uri = InstrumentUtils.getURI(e1);
                String methodName;
                if (sig == null) {
                    methodName = "";
                } else {
                    methodName = sig.getMethodName();
                }

                return new XmlRpcPointCut.XmlRpcTracer(this, transaction, sig, call, e1.getHost(), "XmlRpc", uri,
                                                              new String[] {methodName});
            } catch (MalformedURLException var9) {
                Agent.LOG.log(Level.FINE, "Unable to parse the target endpoint address for an XML RPC call", var9);
            }
        } catch (Throwable var10) {
            Agent.LOG.log(Level.FINE, "Unable to get the target endpoint address for an XML RPC call", var10);
        }

        return null;
    }

    private static final class XmlRpcTracer extends ExternalComponentTracer implements IOTracer {
        private XmlRpcTracer(com.newrelic.agent.instrumentation.PointCut pc, Transaction transaction,
                             ClassMethodSignature sig, Object object, String host, String library, String uri,
                             String[] operations) {
            super(transaction, sig, object, host, library, uri, operations);
        }
    }
}
