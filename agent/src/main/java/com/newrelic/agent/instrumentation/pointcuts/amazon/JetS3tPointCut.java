package com.newrelic.agent.instrumentation.pointcuts.amazon;

import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class JetS3tPointCut extends AbstractJetS3tPointCut {
    public JetS3tPointCut(ClassTransformer classTransformer) {
        super(JetS3tPointCut.class, new MethodMatcher[] {new ExactMethodMatcher("listAllBuckets",
                                                                                       "()[Lorg/jets3t/service/model/S3Bucket;")});
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object service, Object[] args) {
        String host;
        String uri;
        try {
            host = getHost(service);
            uri = getUri(service);
        } catch (Exception e) {
            host = "storage";
            uri = "";
            String msg = MessageFormat.format("Instrumentation error invoking {0} in {1}: {2}",
                                                     new Object[] {sig, getClass().getName(), e});

            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, e);
            } else {
                Agent.LOG.log(Level.FINE, msg);
            }
        }

        return new ExternalComponentTracer(transaction, sig, service, host, "Jets3t", uri,
                                                  new String[] {sig.getMethodName()});
    }
}