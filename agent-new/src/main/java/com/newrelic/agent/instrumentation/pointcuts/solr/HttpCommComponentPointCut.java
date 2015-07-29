package com.newrelic.agent.instrumentation.pointcuts.solr;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class HttpCommComponentPointCut extends AbstractSolrPointCut
{
  public HttpCommComponentPointCut(ClassTransformer classTransformer)
  {
    super(HttpCommComponentPointCut.class, new ExactClassMatcher("org/apache/solr/handler/component/HttpCommComponent"), createExactMethodMatcher("submit", new String[] { "(Lorg/apache/solr/handler/component/ShardRequest;Ljava/lang/String;Lorg/apache/solr/common/params/ModifiableSolrParams;)V" }));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object component, Object[] args)
  {
    return new DefaultTracer(transaction, sig, component, new ClassMethodMetricNameFormat(sig, component, "Solr"));
  }
}