package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionState;
import com.newrelic.agent.async.AsyncTransactionState;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.MethodMapper;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.instrumentation.pointcuts.TransactionHolder;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

@PointCut
public class NettyDispatcherPointCut extends TracerFactoryPointCut
{
  public static final String INSTRUMENTATION_GROUP_NAME = "netty_instrumentation";
  private static final boolean DEFAULT_ENABLED = true;
  private static final String POINT_CUT_NAME = NettyDispatcherPointCut.class.getName();
  static final String CLASS = "org/jboss/netty/handler/codec/frame/FrameDecoder";
  static final String HTTP_CLASS = "org/jboss/netty/handler/codec/http/HttpRequestDecoder";
  static final String METHOD_NAME = "unfoldAndFireMessageReceived";
  static final String METHOD_DESC = "(Lorg/jboss/netty/channel/ChannelHandlerContext;Ljava/net/SocketAddress;Ljava/lang/Object;)V";
  static final String NETTY_DISPATCHER = "NettyDispatcher";
  private final AtomicBoolean firstRun = new AtomicBoolean(true);
  private final String alternateDispatchClassName;

  public NettyDispatcherPointCut(ClassTransformer classTransformer)
  {
    super(createPointCutConfig(), createClassMatcher(), createMethodMatcher());
    this.alternateDispatchClassName = ((String)ServiceFactory.getConfigService().getDefaultAgentConfig().getValue("class_transformer.netty_dispatcher_class"));
  }

  private static PointCutConfiguration createPointCutConfig()
  {
    return new PointCutConfiguration(POINT_CUT_NAME, "netty_instrumentation", true);
  }

  private static ClassMatcher createClassMatcher() {
    return new ExactClassMatcher("org/jboss/netty/handler/codec/frame/FrameDecoder");
  }

  private static MethodMatcher createMethodMatcher() {
    return new ExactMethodMatcher("unfoldAndFireMessageReceived", "(Lorg/jboss/netty/channel/ChannelHandlerContext;Ljava/net/SocketAddress;Ljava/lang/Object;)V");
  }

  protected boolean isDispatcher()
  {
    return true;
  }

  public final Tracer doGetTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args)
  {
    boolean isExpectedType = object instanceof HttpRequestDecoder;
    boolean isRequestPresent = args[2] instanceof NettyHttpRequest;
    if ((this.alternateDispatchClassName != null) && (isRequestPresent) && (!isExpectedType)) {
      isExpectedType = object.getClass().getName().equals(this.alternateDispatchClassName);
    }
    if ((!isExpectedType) || (!isRequestPresent)) {
      Agent.LOG.log(Level.FINEST, "NettyDispatcher: Skipping message {1} recieved for {0}", new Object[] { object.getClass(), args[2] });

      Transaction.clearTransaction();
      return null;
    }
    return buildTracer(tx, sig, object, args);
  }

  Tracer buildTracer(Transaction tx, ClassMethodSignature sig, Object object, Object[] args) {
    if (this.firstRun.get())
    {
      Agent.LOG.fine("Clearing first transaction to allow system to initalize.");
      this.firstRun.set(false);
      Transaction.clearTransaction();
      return null;
    }
    Tracer rootTracer = tx.getRootTracer();
    if (rootTracer != null) {
      Agent.LOG.log(Level.FINER, "NettyDispatcher: rootTracer not null. Already in a transaction? {0}->{1}", new Object[] { tx, rootTracer });

      return null;
    }
    Request httpRequest = DelegatingNettyHttpRequest.create((NettyHttpRequest)args[2]);
    Response httpResponse = DelegatingNettyHttpResponse.create(null);
    Tracer tracer = createTracer(tx, sig, object, httpRequest, httpResponse);
    if (tracer != null) {
      setTransactionName(tx);
    }
    if ((args[0] instanceof ChannelHandlerContext)) {
      ChannelHandlerContext ctx = (ChannelHandlerContext)args[0];
      if ((ctx._nr_getChannel() instanceof TransactionHolder)) {
        TransactionHolder th = (TransactionHolder)ctx._nr_getChannel();
        Agent.LOG.log(Level.FINER, "Setting {0} on holder {1}", new Object[] { tx, th });
        th._nr_setTransaction(tx);
        tx.getTransactionState().asyncJobStarted(th);
      } else {
        Agent.LOG.log(Level.FINER, "Unable to get holder from {1}", new Object[] { ctx });
      }
    } else {
      Agent.LOG.log(Level.FINER, "Invalid context {1}", new Object[] { args[0] });
    }
    return tracer;
  }

  private void setTransactionName(Transaction tx) {
    if (!tx.isTransactionNamingEnabled()) {
      return;
    }
    TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
    if ((Agent.LOG.isLoggable(Level.FINER)) && 
      (policy.canSetTransactionName(tx, TransactionNamePriority.SERVLET_NAME))) {
      String msg = MessageFormat.format("Setting transaction name to \"{0}\" using Netty Http Decoder", new Object[] { "NettyDispatcher" });

      Agent.LOG.finer(msg);
    }

    policy.setTransactionName(tx, "NettyDispatcher", "NettyDispatcher", TransactionNamePriority.SERVLET_NAME);
  }

  private Tracer createTracer(Transaction tx, ClassMethodSignature sig, Object object, Request httpRequest, Response httpResponse)
  {
    TransactionState transactionState = tx.getTransactionState();
    if (!(transactionState instanceof AsyncTransactionState))
      tx.setTransactionState(new AsyncTransactionState(tx.getTransactionActivity()));
    try
    {
      return new BasicRequestRootTracer(tx, sig, object, httpRequest, httpResponse, new SimpleMetricNameFormat("Java/org.jboss.netty.handler.codec.http.HttpRequestDecoder/unfoldAndFireMessageReceived"));
    }
    catch (Exception e) {
      String msg = MessageFormat.format("Unable to create request dispatcher tracer: {0}", new Object[] { e });
      if (Agent.LOG.isFinestEnabled())
        Agent.LOG.log(Level.WARNING, msg, e);
      else
        Agent.LOG.warning(msg);
    }
    return null;
  }

  @InterfaceMixin(originalClassName={"org/jboss/netty/handler/codec/http/HttpRequestDecoder"})
  public static abstract interface HttpRequestDecoder
  {
  }

  @InterfaceMapper(className={"org/jboss/netty/channel/DefaultChannelPipeline$DefaultChannelHandlerContext"}, originalInterfaceName="org/jboss/netty/channel/ChannelHandlerContext")
  public static abstract interface ChannelHandlerContext
  {
    public static final String CLASS = "org/jboss/netty/channel/DefaultChannelPipeline$DefaultChannelHandlerContext";
    public static final String INTERFACE = "org/jboss/netty/channel/ChannelHandlerContext";

    @MethodMapper(originalMethodName="getChannel", originalDescriptor="()Lorg/jboss/netty/channel/Channel;", invokeInterface=false)
    public abstract Object _nr_getChannel();
  }
}