package com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.util.Strings;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

@PointCut
public class CXFInvokerPointCut extends TracerFactoryPointCut
{
  private static final String CXF = "CXF";

  public CXFInvokerPointCut(ClassTransformer classTransformer)
  {
    super(CXFInvokerPointCut.class, new ExactClassMatcher("org/apache/cxf/service/invoker/AbstractInvoker"), createExactMethodMatcher("performInvocation", new String[] { "(Lorg/apache/cxf/message/Exchange;Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;" }));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object invoker, Object[] args)
  {
    Object service = args[1];
    Method method = (Method)args[2];
    String address = (String)transaction.getInternalParameters().remove("cfx_end_point");

    if (address != null) {
      StringBuilder path = new StringBuilder(address);
      if (!address.endsWith("/")) {
        path.append('/');
      }
      path.append(method.getName());
      setTransactionName(transaction, getCXFRequestUri(address, method));
    } else {
      Agent.LOG.log(Level.FINEST, "The CXF endpoint address is null.");
      setTransactionName(transaction, service.getClass().getName() + '/' + method.getName());
    }

    return new DefaultTracer(transaction, sig, invoker, new SimpleMetricNameFormat(Strings.join('/', new String[] { "Java", service.getClass().getName(), method.getName() })));
  }

  static String getCXFRequestUri(String address, Method method)
  {
    try {
      address = new URI(address).getPath();
    } catch (URISyntaxException e) {
    }
    StringBuilder path = new StringBuilder();
    if (!address.startsWith("/")) {
      path.append('/');
    }
    path.append(address);
    if (!address.endsWith("/")) {
      path.append('/');
    }
    path.append(method.getName());
    return path.toString();
  }

  private void setTransactionName(Transaction transaction, String path) {
    if (!transaction.isTransactionNamingEnabled()) {
      return;
    }
    TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
    if ((Agent.LOG.isLoggable(Level.FINER)) && 
      (policy.canSetTransactionName(transaction, TransactionNamePriority.FRAMEWORK))) {
      String msg = MessageFormat.format("Setting transaction name to \"{0}\" using CXF", new Object[] { path });
      Agent.LOG.finer(msg);
    }

    policy.setTransactionName(transaction, path, "CXF", TransactionNamePriority.FRAMEWORK);
  }
}