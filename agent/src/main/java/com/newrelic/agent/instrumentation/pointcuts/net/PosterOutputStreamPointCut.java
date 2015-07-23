//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.net;

import java.net.URL;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.InstrumentUtils;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.IOTracer;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class PosterOutputStreamPointCut extends TracerFactoryPointCut {
    public static final String POSTER_OUTPUT_STREAM_CLASS_NAME = "sun/net/www/http/PosterOutputStream";
    private static final String POINT_CUT_NAME = "http_output_stream";
    private static final boolean DEFAULT_ENABLED = false;
    private static final String WRITE_METHOD_NAME = "write";
    private static final String WRITE_METHOD_DESC = "([BII)V";

    public PosterOutputStreamPointCut(ClassTransformer classTransformer) {
        super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
        classTransformer.getClassNameFilter().addIncludeClass("sun/net/www/http/PosterOutputStream");
    }

    private static PointCutConfiguration createPointCutConfig() {
        return new PointCutConfiguration("http_output_stream", false);
    }

    private static ClassMatcher createClassMatcher() {
        return new ExactClassMatcher("sun/net/www/http/PosterOutputStream");
    }

    private static MethodMatcher createMethodMatcher() {
        return new ExactMethodMatcher("write", "([BII)V");
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object outputStream, Object[] args) {
        URL url = transaction.getTransactionCache().getURL(outputStream);
        String host;
        String uri;
        if (url != null) {
            host = url.getHost();
            uri = InstrumentUtils.getURI(url);
        } else {
            host = "unknown";
            uri = "";
        }

        String operation;
        if (sig != null) {
            operation = sig.getMethodName();
        } else {
            operation = "";
        }

        return new PosterOutputStreamPointCut.HttpOutputStreamTracer(this, transaction, sig, outputStream, host,
                                                                            "HttpOutputStream", uri,
                                                                            new String[] {operation});
    }

    private static final class HttpOutputStreamTracer extends ExternalComponentTracer implements IOTracer {
        private HttpOutputStreamTracer(com.newrelic.agent.instrumentation.PointCut pc, Transaction transaction,
                                       ClassMethodSignature sig, Object object, String host, String library, String uri,
                                       String[] operations) {
            super(transaction, sig, object, host, library, uri, operations);
        }
    }
}
