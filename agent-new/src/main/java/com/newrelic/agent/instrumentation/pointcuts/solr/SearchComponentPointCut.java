package com.newrelic.agent.instrumentation.pointcuts.solr;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.transaction.TransactionCache;

@PointCut
public class SearchComponentPointCut extends AbstractSolrPointCut
{
  public SearchComponentPointCut(ClassTransformer classTransformer)
  {
    super(SearchComponentPointCut.class, new ChildClassMatcher("org/apache/solr/handler/component/SearchComponent"), OrMethodMatcher.getMethodMatcher(new MethodMatcher[] { new ExactMethodMatcher("handleResponses", "(Lorg/apache/solr/handler/component/ResponseBuilder;Lorg/apache/solr/handler/component/ShardRequest;)V"), new ExactMethodMatcher("prepare", "(Lorg/apache/solr/handler/component/ResponseBuilder;)V"), new ExactMethodMatcher("process", "(Lorg/apache/solr/handler/component/ResponseBuilder;)V") }));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object component, Object[] args)
  {
    transaction.getTransactionCache().putSolrResponseBuilderParamName(args[0]);

    return new DefaultTracer(transaction, sig, component, new ClassMethodMetricNameFormat(sig, component, "Solr"));
  }
}