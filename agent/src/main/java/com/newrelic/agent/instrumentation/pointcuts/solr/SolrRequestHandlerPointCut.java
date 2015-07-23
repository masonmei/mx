package com.newrelic.agent.instrumentation.pointcuts.solr;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OrClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.DefaultTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.metricname.ClassMethodMetricNameFormat;
import com.newrelic.agent.transaction.TransactionNamingPolicy;

@PointCut
public class SolrRequestHandlerPointCut extends AbstractSolrPointCut {
    private static final String SOLR = "Solr";
    private static SolrReflectionHelper sSolrReflectionHelper = null;

    public SolrRequestHandlerPointCut(ClassTransformer classTransformer) {
        super(SolrRequestHandlerPointCut.class, OrClassMatcher
                                                        .getClassMatcher(new ClassMatcher[] {new ExactClassMatcher
                                                                                                     ("org/apache/solr/handler/RequestHandlerBase"),
                                                                                                    new InterfaceMatcher("org/apache/solr/request/SolrRequestHandler")}),
                     createExactMethodMatcher("handleRequest",
                                                     new String[] {"(Lorg/apache/solr/request/SolrQueryRequest;"
                                                                           +
                                                                           "Lorg/apache/solr/request/SolrQueryResponse;)V"}));
    }

    static SolrReflectionHelper getSolrReflectionHelper(Object rb) throws Exception {
        if (sSolrReflectionHelper == null) {
            sSolrReflectionHelper = new SolrReflectionHelper(rb);
        }

        return sSolrReflectionHelper;
    }

    static Map<String, String> addDebugInfo(Object rb) {
        Map result = new HashMap();
        try {
            SolrReflectionHelper helper = getSolrReflectionHelper(rb);

            Object solrQueryRequest = helper.reqField.get(rb);

            Object solrParams = helper.getParamsMethod.invoke(solrQueryRequest, new Object[0]);
            String rawQueryString = (String) solrParams.getClass().getMethod("get", new Class[] {String.class})
                                                     .invoke(solrParams, new Object[] {"q"});

            result.put("library.solr.raw_query_string", rawQueryString);

            String queryString = (String) helper.getQueryString.invoke(rb, new Object[0]);
            result.put("library.solr.query_string", queryString);

            Object schema = helper.getSchemaMethod.invoke(solrQueryRequest, new Object[0]);
            Object query = helper.getQuery.invoke(rb, new Object[0]);
            String parsedQuery = (String) helper.queryParsingClass.getMethod("toString", new Class[] {helper.queryClass,
                                                                                                             helper.indexSchemaClass})
                                                  .invoke(null, new Object[] {query, schema});

            result.put("library.solr.lucene_query", parsedQuery);

            String parsedQueryToString = query.toString();
            result.put("library.solr.lucene_query_string", parsedQueryToString);
        } catch (Throwable e) {
            result.put("library.solr.solr_debug_info_error", e.toString());

            String msg = MessageFormat.format("Error in Solr debug data collection - {0}", new Object[] {e.toString()});
            Agent.LOG.finer(msg);
            Agent.LOG.log(Level.FINEST, msg, e);
        }

        return result;
    }

    public Tracer doGetTracer(final Transaction transaction, ClassMethodSignature sig, Object handler, Object[] args) {
        final String type = getQueryType(handler, args[0]);

        return new DefaultTracer(transaction, sig, handler,
                                        new ClassMethodMetricNameFormat(sig, handler, "SolrRequestHandler")) {
            protected void doFinish(int opcode, Object returnValue) {
                super.doFinish(opcode, returnValue);
                Dispatcher dispatcher = transaction.getDispatcher();

                Object rb = transaction.getTransactionCache().removeSolrResponseBuilderParamName();

                if (rb != null) {
                    Map result = SolrRequestHandlerPointCut.addDebugInfo(rb);

                    transaction.getAgentAttributes().putAll(result);
                }

                if (dispatcher.isWebTransaction()) {
                    String uri = dispatcher.getUri();
                    StringBuilder builder = new StringBuilder();
                    builder.append(uri);
                    if (type != null) {
                        if (!uri.endsWith("/")) {
                            builder.append('/');
                        }
                        builder.append(type);
                    }
                    setTransactionName(transaction, builder.toString());
                }
            }

            private void setTransactionName(Transaction tx, String uri) {
                if (!tx.isTransactionNamingEnabled()) {
                    return;
                }
                TransactionNamingPolicy policy = TransactionNamingPolicy.getHigherPriorityTransactionNamingPolicy();
                if (policy.canSetTransactionName(tx, TransactionNamePriority.FRAMEWORK)) {
                    uri = ServiceFactory.getNormalizationService().getUrlNormalizer(tx.getApplicationName())
                                  .normalize(uri);

                    if (uri == null) {
                        tx.setIgnore(true);
                    } else {
                        if (Agent.LOG.isLoggable(Level.FINER)) {
                            String msg = MessageFormat
                                                 .format("Setting transaction name to \"{0}\" using Solr request URI",
                                                                new Object[] {uri});

                            Agent.LOG.finer(msg);
                        }
                        policy.setTransactionName(transaction, uri, "Solr", TransactionNamePriority.FRAMEWORK);
                    }
                }
            }
        };
    }

    private String getQueryType(Object handler, Object request) {
        try {
            Object queryType = request.getClass().getClassLoader().loadClass("org.apache.solr.request.SolrQueryRequest")
                                       .getMethod("getQueryType", new Class[0]).invoke(request, new Object[0]);

            if (queryType != null) {
                return queryType.toString();
            }
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to get the SolrQueryRequest query type : {0}",
                                                     new Object[] {e.toString()});
            Agent.LOG.info(msg);
            Agent.LOG.log(Level.FINER, msg, e);
        }
        return null;
    }

    private static class SolrReflectionHelper {
        public final Class<?> solrPluginUtilsClass;
        public final Class<?> solrQueryRequestClass;
        public final Class<?> docListClass;
        public final Class<?> queryClass;
        public final Class<?> queryParsingClass;
        public final Class<?> indexSchemaClass;
        public final Field reqField;
        public final Method getQueryString;
        public final Method getQuery;
        public final Method getResults;
        public final Method getParamsMethod;
        public final Method getSchemaMethod;

        public SolrReflectionHelper(Object rb) throws Exception {
            Class responseBuilderClass = rb.getClass();

            ClassLoader cl = responseBuilderClass.getClassLoader();

            solrPluginUtilsClass = cl.loadClass("org.apache.solr.util.SolrPluginUtils");
            solrQueryRequestClass = cl.loadClass("org.apache.solr.request.SolrQueryRequest");
            docListClass = cl.loadClass("org.apache.solr.search.DocList");
            queryClass = cl.loadClass("org.apache.lucene.search.Query");
            queryParsingClass = cl.loadClass("org.apache.solr.search.QueryParsing");
            indexSchemaClass = cl.loadClass("org.apache.solr.schema.IndexSchema");

            reqField = responseBuilderClass.getDeclaredField("req");
            getQueryString = responseBuilderClass.getMethod("getQueryString", new Class[0]);
            getQuery = responseBuilderClass.getMethod("getQuery", new Class[0]);
            getResults = responseBuilderClass.getMethod("getResults", new Class[0]);
            getParamsMethod = solrQueryRequestClass.getMethod("getParams", new Class[0]);
            getSchemaMethod = solrQueryRequestClass.getMethod("getSchema", new Class[0]);
        }
    }
}