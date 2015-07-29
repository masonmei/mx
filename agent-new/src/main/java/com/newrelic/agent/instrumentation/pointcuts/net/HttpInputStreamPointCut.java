//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.net;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.InstrumentUtils;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.IOTracer;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class HttpInputStreamPointCut extends TracerFactoryPointCut {
  public static final String HTTP_INPUT_STREAM_CLASS_NAME =
          "sun/net/www/protocol/http/HttpURLConnection$HttpInputStream";

  public HttpInputStreamPointCut(ClassTransformer classTransformer) {
    super(new PointCutConfiguration("http_input_stream", (String) null, false),
                 new ExactClassMatcher("sun/net/www/protocol/http/HttpURLConnection$HttpInputStream"),
                 createExactMethodMatcher("read", new String[] {"([BII)I"}));
    classTransformer.getClassNameFilter()
            .addIncludeClass("sun/net/www/protocol/http/HttpURLConnection$HttpInputStream");
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object inputStream, Object[] args) {
    URL url = transaction.getTransactionCache().getURL(inputStream);
    if (url == null) {
      try {
        Method host = HttpURLConnection.class.getMethod("getURL", new Class[0]);
        Field uri = inputStream.getClass().getDeclaredField("this$0");
        uri.setAccessible(true);
        HttpURLConnection operation = (HttpURLConnection) uri.get(inputStream);
        url = (URL) host.invoke(operation, new Object[0]);
        transaction.getTransactionCache().putURL(inputStream, url);
      } catch (Throwable var9) {
        Agent.LOG.log(Level.FINER, "Error getting url from http input stream", var9);
      }
    }

    String host1;
    String uri1;
    if (url != null) {
      host1 = url.getHost();
      uri1 = InstrumentUtils.getURI(url);
    } else {
      host1 = "unknown";
      uri1 = "";
    }

    String operation1;
    if (sig != null) {
      operation1 = sig.getMethodName();
    } else {
      operation1 = "";
    }

    return new HttpInputStreamPointCut.HttpInputStreamTracer(this, transaction, sig, inputStream, host1,
                                                                    "HttpInputStream", uri1,
                                                                    new String[] {operation1});
  }

  private static final class HttpInputStreamTracer extends ExternalComponentTracer implements IOTracer {
    private HttpInputStreamTracer(com.newrelic.agent.instrumentation.PointCut pc, Transaction transaction,
                                  ClassMethodSignature sig, Object object, String host, String library, String uri,
                                  String[] operations) {
      super(transaction, sig, object, host, library, uri, operations);
    }
  }
}
