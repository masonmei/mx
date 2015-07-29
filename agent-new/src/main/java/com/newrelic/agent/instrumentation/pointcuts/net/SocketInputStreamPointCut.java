//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.net;

import java.lang.reflect.Field;
import java.net.Socket;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionSegment;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentPointCut;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.IOTracer;
import com.newrelic.agent.tracers.MetricNameFormatWithHost;
import com.newrelic.agent.tracers.Tracer;

@PointCut
public class SocketInputStreamPointCut extends ExternalComponentPointCut {
  public static final String[] INPUT_STREAM_METHODS = new String[] {"()I", "([BII)I"};
  public static final String SOCKET_INPUT_STREAM_CLASS_NAME = "java/net/SocketInputStream";

  public SocketInputStreamPointCut(ClassTransformer classTransformer) {
    super(new PointCutConfiguration("socket_input_stream", (String) null, false),
                 new ExactClassMatcher("java/net/SocketInputStream"),
                 createExactMethodMatcher("read", INPUT_STREAM_METHODS));
    classTransformer.getClassNameFilter().addIncludeClass("java/net/SocketInputStream");
  }

  protected Tracer getExternalTracer(Transaction transaction, ClassMethodSignature sig, Object inputStream,
                                     Object[] args) {
    MetricNameFormatWithHost metricFormat =
            transaction.getTransactionCache().getMetricNameFormatWithHost(inputStream);
    if (metricFormat == null) {
      try {
        Field t = inputStream.getClass().getDeclaredField("socket");
        t.setAccessible(true);
        Socket socket = (Socket) t.get(inputStream);
        String host = socket.getInetAddress().getHostName();
        metricFormat = MetricNameFormatWithHost.create(host, "SocketInputStream");
        transaction.getTransactionCache().putMetricNameFormatWithHost(inputStream, metricFormat);
      } catch (Throwable var9) {
        metricFormat = MetricNameFormatWithHost.create("errorFetchingHost", "SocketInputStream");
        transaction.getTransactionCache().putMetricNameFormatWithHost(inputStream, metricFormat);
        Agent.LOG.log(Level.FINER, "Error getting url from http input stream", var9);
      }
    }

    return new SocketInputStreamPointCut.SocketInputStreamTracer(this, transaction, sig, inputStream, metricFormat);
  }

  public void noticeTransformerStarted(ClassTransformer classTransformer) {
    this.avoidUnsatisfiedLinkError();
    InstrumentationProxy instrumentation = ServiceFactory.getAgent().getInstrumentation();

    try {
      instrumentation.retransformUninstrumentedClasses(new String[] {"java.net.SocketInputStream"});
    } catch (Exception var4) {
      Agent.LOG.log(Level.FINER, "Unable to retransform SocketInputStream", var4);
    }

  }

  private void avoidUnsatisfiedLinkError() {
    Socket sock = null;

    try {
      sock = new Socket();
      sock.getInputStream();
    } catch (Exception var5) {
      if (sock != null) {
        try {
          sock.close();
        } catch (Exception var4) {
          ;
        }
      }
    }

  }

  private static final class SocketInputStreamTracer extends ExternalComponentTracer implements IOTracer {
    private SocketInputStreamTracer(com.newrelic.agent.instrumentation.PointCut pc, Transaction transaction,
                                    ClassMethodSignature sig, Object object,
                                    MetricNameFormatWithHost metricNameFormat) {
      super(transaction, sig, object, metricNameFormat.getHost(), metricNameFormat);
    }

    public TransactionSegment getTransactionSegment(TransactionTracerConfig ttConfig, SqlObfuscator sqlObfuscator,
                                                    long startTime, TransactionSegment lastSibling) {
      if (lastSibling != null && lastSibling.getMetricName().equals(this.getTransactionSegmentName())) {
        lastSibling.merge(this);
        return lastSibling;
      } else {
        return super.getTransactionSegment(ttConfig, sqlObfuscator, startTime, lastSibling);
      }
    }
  }
}
