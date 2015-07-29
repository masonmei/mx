//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.solr;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMixin;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.servlet.ServletUtils;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.IgnoreChildSocketCalls;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;

@PointCut
public class SolrServerPointCut extends AbstractSolrPointCut {
  private static final String QUERY_METHOD_NAME = "query";
  private static final String NO_ARG_DESCRIPTION = "()Lorg/apache/solr/client/solrj/response/UpdateResponse;";

  public SolrServerPointCut(ClassTransformer classTransformer) {
    super(SolrServerPointCut.class, new ExactClassMatcher("org/apache/solr/client/solrj/SolrServer"),
                 OrMethodMatcher.getMethodMatcher(new MethodMatcher[] {new ExactMethodMatcher("add",
                                                                                                     new String[]
                                                                                                             {"(Ljava/util/Collection;)Lorg/apache/solr/client/solrj/response/UpdateResponse;",
                                                                                                                     "(Lorg/apache/solr/common/SolrInputDocument;)Lorg/apache/solr/client/solrj/response/UpdateResponse;"}),
                                                                              new ExactMethodMatcher("commit",
                                                                                                            new String[] {"()Lorg/apache/solr/client/solrj/response/UpdateResponse;",
                                                                                                                                 "(ZZ)Lorg/apache/solr/client/solrj/response/UpdateResponse;"}),
                                                                              new ExactMethodMatcher("optimize",
                                                                                                            new String[] {"()Lorg/apache/solr/client/solrj/response/UpdateResponse;",
                                                                                                                                 "(ZZ)Lorg/apache/solr/client/solrj/response/UpdateResponse;",
                                                                                                                                 "(ZZI)Lorg/apache/solr/client/solrj/response/UpdateResponse;"}),
                                                                              new ExactMethodMatcher("rollback",
                                                                                                            "()Lorg/apache/solr/client/solrj/response/UpdateResponse;"),
                                                                              new ExactMethodMatcher("deleteById",
                                                                                                            new String[] {"(Ljava/lang/String;)Lorg/apache/solr/client/solrj/response/UpdateResponse;",
                                                                                                                                 "(Ljava/util/List;)Lorg/apache/solr/client/solrj/response/UpdateResponse;"}),
                                                                              new ExactMethodMatcher
                                                                                      ("deleteByQuery",
                                                                                              "(Ljava/lang/String;)Lorg/apache/solr/client/solrj/response/UpdateResponse;"),
                                                                              new ExactMethodMatcher("ping",
                                                                                                            "()Lorg/apache/solr/client/solrj/response/SolrPingResponse;"),
                                                                              new ExactMethodMatcher("query",
                                                                                                            new String[] {"(Lorg/apache/solr/common/params/SolrParams;)Lorg/apache/solr/client/solrj/response/QueryResponse;",
                                                                                                                                 "(Lorg/apache/solr/common/params/SolrParams;Lorg/apache/solr/client/solrj/SolrRequest$METHOD;)Lorg/apache/solr/client/solrj/response/QueryResponse;"})}));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object server, Object[] args) {
    return new SolrServerPointCut.SolrServerTracer(transaction, sig, server, args);
  }

  @InterfaceMixin(
                         originalClassName = {"org/apache/solr/common/params/SolrParams"})
  public interface SolrParams {
    String[] getParams(String var1);

    Iterator<String> getParameterNamesIterator();
  }

  private class SolrServerTracer extends DefaultTracer implements IgnoreChildSocketCalls {
    public SolrServerTracer(Transaction transaction, ClassMethodSignature sig, Object server, Object[] args) {
      super(transaction, sig, server, new ClassMethodMetricNameFormat(sig, server, "SolrClient"));
      if ("query".equals(sig.getMethodName())) {
        Object solrParams = args[0];

        try {
          HashMap e = new HashMap();
          String paramName;
          if (solrParams instanceof SolrServerPointCut.SolrParams) {
            Iterator paramsClass1 =
                    ((SolrServerPointCut.SolrParams) solrParams).getParameterNamesIterator();

            while (paramsClass1.hasNext()) {
              paramName = (String) paramsClass1.next();
              e.put(paramName, ((SolrServerPointCut.SolrParams) solrParams).getParams(paramName));
            }
          } else {
            Class paramsClass = solrParams.getClass();
            Method paramNamesIter = paramsClass.getMethod("getParameterNamesIterator", new Class[0]);
            Method getParams = paramsClass.getMethod("getParams", new Class[] {String.class});
            Iterator iter = (Iterator) paramNamesIter.invoke(solrParams, new Object[0]);

            while (iter.hasNext()) {
              paramName = (String) iter.next();
              String[] values =
                      (String[]) ((String[]) getParams.invoke(solrParams, new Object[] {paramName}));
              e.put(paramName, values);
            }
          }

          if (!e.isEmpty()) {
            this.setAttribute("query_params", ServletUtils.getSimpleParameterMap(e, transaction
                                                                                            .getAgentConfig()
                                                                                            .getMaxUserParameterSize()));
          }
        } catch (Exception var14) {
          Agent.LOG.log(Level.FINER, "Solr client error", var14);
        }
      }

    }

    protected void doRecordMetrics(TransactionStats transactionStats) {
      transactionStats.getUnscopedStats().getResponseTimeStats("Solr/all")
              .recordResponseTime(this.getExclusiveDuration(), TimeUnit.NANOSECONDS);
      transactionStats.getUnscopedStats().getResponseTimeStats((this.getTransaction().isWebTransaction()
                                                                        ? MetricName.WEB_TRANSACTION_SOLR_ALL
                                                                        : MetricName.OTHER_TRANSACTION_SOLR_ALL)
                                                                       .getName())
              .recordResponseTime(this.getExclusiveDuration(), TimeUnit.NANOSECONDS);
    }
  }
}
