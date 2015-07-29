//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.amazon;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.agent.Transaction;
import com.newrelic.agent.instrumentation.ClassTransformer;
import com.newrelic.agent.instrumentation.TracerFactoryPointCut;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.OrMethodMatcher;
import com.newrelic.agent.instrumentation.pointcuts.PointCut;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ExternalComponentTracer;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;

@PointCut
public class AmazonS3ConectionPointCut extends TracerFactoryPointCut {
  private static final Map<MethodMatcher, TracerFactory> methodMatcherTracers =
          Collections.unmodifiableMap(createTracerFactories());

  public AmazonS3ConectionPointCut(ClassTransformer classTransformer) {
    super(AmazonS3ConectionPointCut.class, new ExactClassMatcher("com/amazon/s3/AWSAuthConnection"), OrMethodMatcher
                                                                                                             .getMethodMatcher((MethodMatcher[]) methodMatcherTracers
                                                                                                                                                         .keySet()
                                                                                                                                                         .toArray(new MethodMatcher[0])));
  }

  private static Map<MethodMatcher, TracerFactory> createTracerFactories() {
    HashMap factories = new HashMap() {
      {
        AmazonS3ConectionPointCut.addBasicTracerFactory(this, "listAllMyBuckets", "(Ljava/util/Map;)"
                                                                                          +
                                                                                          "Lcom/amazon/s3/ListAllMyBucketsResponse;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "createBucket",
                                                                "(Ljava/lang/String;Ljava/util/Map;)"
                                                                        + "Lcom/amazon/s3/Response;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "createBucket",
                                                                "(Ljava/lang/String;Ljava/lang/String;"
                                                                        + "Ljava/util/Map;)"
                                                                        + "Lcom/amazon/s3/Response;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "listBucket",
                                                                "(Ljava/lang/String;Ljava/lang/String;"
                                                                        + "Ljava/lang/String;"
                                                                        + "Ljava/lang/Integer;Ljava/util/Map;"
                                                                        + ")Lcom/amazon/s3/ListBucketResponse;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "listBucket",
                                                                "(Ljava/lang/String;Ljava/lang/String;"
                                                                        + "Ljava/lang/String;"
                                                                        + "Ljava/lang/Integer;"
                                                                        + "Ljava/lang/String;Ljava/util/Map;)"
                                                                        + "Lcom/amazon/s3/ListBucketResponse;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "deleteBucket",
                                                                "(Ljava/lang/String;Ljava/util/Map;)"
                                                                        + "Lcom/amazon/s3/Response;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "getBucketLocation", "(Ljava/lang/String;)"
                                                                                            +
                                                                                            "Lcom/amazon/s3/LocationResponse;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "get", "(Ljava/lang/String;Ljava/lang/String;"
                                                                              + "Ljava/util/Map;)"
                                                                              + "Lcom/amazon/s3/GetResponse;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "put", "(Ljava/lang/String;Ljava/lang/String;"
                                                                              + "Lcom/amazon/s3/S3Object;"
                                                                              + "Ljava/util/Map;)"
                                                                              + "Lcom/amazon/s3/Response;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "copy", "(Ljava/lang/String;Ljava/lang/String;"
                                                                               + "Ljava/lang/String;"
                                                                               + "Ljava/lang/String;"
                                                                               + "Ljava/util/Map;)"
                                                                               + "Lcom/amazon/s3/Response;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "copy", "(Ljava/lang/String;Ljava/lang/String;"
                                                                               + "Ljava/lang/String;"
                                                                               + "Ljava/lang/String;"
                                                                               + "Ljava/util/Map;"
                                                                               + "Ljava/util/Map;)"
                                                                               + "Lcom/amazon/s3/Response;");
        AmazonS3ConectionPointCut.addBucketTracerFactory(this, "delete", "(Ljava/lang/String;Ljava/lang/String;"
                                                                                 + "Ljava/util/Map;)"
                                                                                 + "Lcom/amazon/s3/Response;");
      }
    };
    return factories;
  }

  private static void addBasicTracerFactory(Map<MethodMatcher, TracerFactory> map, String methodName,
                                            String methodDesc) {
    methodName = methodName.intern();
    methodDesc = methodDesc.intern();
    map.put(new ExactMethodMatcher(methodName, methodDesc),
                   new AmazonS3ConectionPointCut.BasicTracerFactory(methodName));
  }

  private static void addBucketTracerFactory(Map<MethodMatcher, TracerFactory> map, String methodName,
                                             String methodDesc) {
    methodName = methodName.intern();
    methodDesc = methodDesc.intern();
    map.put(new ExactMethodMatcher(methodName, methodDesc),
                   new AmazonS3ConectionPointCut.BucketTracerFactory(methodName));
  }

  public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
    Iterator i$ = methodMatcherTracers.entrySet().iterator();

    Entry entry;
    do {
      if (!i$.hasNext()) {
        return null;
      }

      entry = (Entry) i$.next();
    } while (!((MethodMatcher) entry.getKey()).matches(-1, sig.getMethodName(), sig.getMethodDesc(),
                                                              MethodMatcher.UNSPECIFIED_ANNOTATIONS));

    return ((TracerFactory) entry.getValue()).getTracer(transaction, sig, object, args);
  }

  private static class BucketTracerFactory extends AmazonS3ConectionPointCut.BasicTracerFactory {
    public BucketTracerFactory(String operation) {
      super(operation);
    }

    public Tracer getTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
      String host = "amazon";
      String uri = "";
      return new ExternalComponentTracer(transaction, sig, object, host, "S3", uri,
                                                new String[] {this.getOperation(), (String) args[0]});
    }
  }

  private static class BasicTracerFactory extends AbstractTracerFactory {
    private final String operation;

    public BasicTracerFactory(String operation) {
      this.operation = operation;
    }

    public String getOperation() {
      return this.operation;
    }

    public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
      String host = "amazon";
      String uri = "";
      return new ExternalComponentTracer(transaction, sig, object, host, "S3", uri,
                                                new String[] {this.getOperation()});
    }
  }
}
