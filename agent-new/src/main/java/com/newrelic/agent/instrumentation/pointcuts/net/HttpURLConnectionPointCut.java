package com.newrelic.agent.instrumentation.pointcuts.net;

import com.newrelic.agent.Agent;
import com.newrelic.agent.IAgent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.bridge.CrossProcessState;
import com.newrelic.agent.instrumentation.ClassNameFilter;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.InstrumentUtils;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.MethodExitTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.transaction.TransactionCache;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.OutboundHeaders;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

@PointCut
public class HttpURLConnectionPointCut extends TracerFactoryPointCut
{
  private static final String POINT_CUT_NAME = HttpURLConnectionPointCut.class.getName();
  private static final boolean DEFAULT_ENABLED = true;
  private static final boolean ONLY_MATCH_CHILDREN = false;
  private static final String CONNECT_METHOD_NAME = "connect";
  private static final String CONNECT_METHOD_DESC = "()V";
  private static final String GET_INPUT_STREAM_METHOD_NAME = "getInputStream";
  private static final String GET_INPUT_STREAM_METHOD_DESC = "()Ljava/io/InputStream;";
  private static final String GET_OUTPUT_STREAM_METHOD_NAME = "getOutputStream";
  private static final String GET_OUTPUT_STREAM_METHOD_DESC = "()Ljava/io/OutputStream;";
  private static final String GET_RESPONSE_CODE_METHOD_NAME = "getResponseCode";
  private static final String GET_RESPONSE_CODE_METHOD_DESC = "()I";
  private static final String HTTP_URL_CONNECTION_CLASS_NAME = "java/net/HttpURLConnection";
  private static final String TO_INCLUDE_CHILD = "sun/net/www/protocol/http/HttpURLConnection";

  public HttpURLConnectionPointCut(ClassTransformer classTransformer)
  {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    classTransformer.getClassNameFilter().addIncludeClass("java/net/HttpURLConnection");
  }

  private static PointCutConfiguration createPointCutConfig() {
    return new PointCutConfiguration(POINT_CUT_NAME, true);
  }

  private static ClassMatcher createClassMatcher() {
    return new ChildClassMatcher("java/net/HttpURLConnection", false, new String[] { "sun/net/www/protocol/http/HttpURLConnection" });
  }

  private static MethodMatcher createMethodMatcher()
  {
    return OrMethodMatcher.getMethodMatcher(new MethodMatcher[] { new ExactMethodMatcher("connect", "()V"), new ExactMethodMatcher("getResponseCode", "()I"), new ExactMethodMatcher("getInputStream", "()Ljava/io/InputStream;"), new ExactMethodMatcher("getOutputStream", "()Ljava/io/OutputStream;") });
  }

  public void noticeTransformerStarted(ClassTransformer classTransformer)
  {
    try
    {
      ServiceFactory.getAgent().getInstrumentation().retransformUninstrumentedClasses(new String[] { "java/net/HttpURLConnection".replaceAll("/", ".") });
    }
    catch (Throwable t) {
      String msg = MessageFormat.format("Error retransforming {0}: {1}", new Object[] { "java/net/HttpURLConnection", t });
      Agent.LOG.info(msg);
    }
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object connection, Object[] args)
  {
    HttpURLConnection urlConnection = (HttpURLConnection)connection;
    final URL url = urlConnection.getURL();
    String uri = InstrumentUtils.getURI(url);
    String host = url.getHost();
    String methodName = sig.getMethodName();
    boolean connected = isConnected(urlConnection);

    String idHeader = urlConnection.getRequestProperty("X-NewRelic-ID");
    Agent.LOG.log(Level.FINEST, "HttpUrlConnection getTransaction {0}: connected : {1}, new relic header: {2}", new Object[] { sig.getMethodName(), Boolean.valueOf(connected), idHeader });

    if ((!connected) && (null == idHeader)) {
      if ("getOutputStream" == sig.getMethodName()) {
        return getOutputStreamConnectionTracer(transaction, sig, urlConnection, uri, host, methodName);
      }
      return getConnectionTracer(transaction, sig, urlConnection, uri, host, methodName);
    }if ("connect" == sig.getMethodName())
      return null;
    if ("getOutputStream" == sig.getMethodName()) {
      return new MethodExitTracer(sig, transaction)
      {
        protected void doFinish(int opcode, Object returnValue) {
          getTransaction().getTransactionCache().putURL(returnValue, url);
        }
      };
    }
    return getResponseCodeTracer(transaction, sig, urlConnection, uri, host, methodName);
  }

  private Tracer getConnectionTracer(Transaction transaction, ClassMethodSignature sig, HttpURLConnection urlConnection, String uri, String host, String methodName)
  {
    if (!urlConnection.getClass().getName().contains("weblogic.net.http.SOAPHttpURLConnection")) {
      transaction.getCrossProcessState().processOutboundRequestHeaders(new OutboundHeadersWrapper(urlConnection));
    }
    return new ExternalComponentTracer(transaction, sig, urlConnection, host, "HttpURLConnection", uri, new String[] { methodName });
  }

  private Tracer getOutputStreamConnectionTracer(Transaction transaction, ClassMethodSignature sig, HttpURLConnection urlConnection, String uri, String host, String methodName)
  {
    final URL url = urlConnection.getURL();
    transaction.getCrossProcessState().processOutboundRequestHeaders(new OutboundHeadersWrapper(urlConnection));
    return new ExternalComponentTracer(transaction, sig, urlConnection, host, "HttpURLConnection", uri, new String[] { methodName })
    {
      protected void doFinish(int opcode, Object returnValue) {
        super.doFinish(opcode, returnValue);
        getTransaction().getTransactionCache().putURL(returnValue, url);
      }
    };
  }

  private Tracer getResponseCodeTracer(Transaction transaction, ClassMethodSignature sig, HttpURLConnection urlConnection, String uri, String host, String methodName)
  {
    List tracers = transaction.getTransactionActivity().getTracers();
    if ((tracers.size() == 0) || ((tracers.get(tracers.size() - 1) instanceof HttpURLConnectionTracer))) {
      return null;
    }
    return new HttpURLConnectionTracer(transaction, sig, urlConnection, host, "HttpURLConnection", uri, methodName);
  }

  private boolean isConnected(HttpURLConnection connection)
  {
    try
    {
      connection.setAllowUserInteraction(connection.getAllowUserInteraction());
      return false; } catch (IllegalStateException ise) {
    }
    return true;
  }

  private class OutboundHeadersWrapper implements OutboundHeaders
  {
    private final HttpURLConnection connection;

    public OutboundHeadersWrapper(HttpURLConnection connection)
    {
      this.connection = connection;
    }

    public void setHeader(String name, String value)
    {
      this.connection.setRequestProperty(name, value);
    }

    public HeaderType getHeaderType()
    {
      return HeaderType.HTTP;
    }
  }
}