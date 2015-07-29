package com.newrelic.agent.attributes;

public final class AttributeNames
{
  public static final String CLIENT_CROSS_PROCESS_ID_PARAMETER_NAME = "client_cross_process_id";
  public static final String EXPLAIN_PLAN_CLAMP = "explain_plan_clamp";
  public static final String REFERRING_TRANSACTION_TRACE_ID_PARAMETER_NAME = "referring_transaction_guid";
  public static final String TRIP_ID_PARAMETER_NAME = "trip_id";
  public static final String PATH_HASH_PARAMETER_NAME = "path_hash";
  public static final String SEGMENT_CLAMP = "segment_clamp";
  public static final String SIZE_LIMIT_PARAMETER_NAME = "size_limit";
  public static final String STACK_TRACE_CLAMP = "stack_trace_clamp";
  public static final String TRANSACTION_TRACE_ID_PARAMETER_NAME = "transaction_guid";
  public static final String CPU_TIME_PARAMETER_NAME = "cpu_time";
  public static final String GC_TIME_PARAMETER_NAME = "gc_time";
  public static final String SYNTHETICS_RESOURCE_ID = "synthetics_resource_id";
  public static final String SYNTHETICS_MONITOR_ID = "synthetics_monitor_id";
  public static final String SYNTHETICS_JOB_ID = "synthetics_job_id";
  public static final String HTTP_REQUEST_PREFIX = "request.parameters.";
  public static final String HTTP_STATUS = "httpResponseCode";
  public static final String HTTP_STATUS_MESSAGE = "httpResponseMessage";
  public static final String LOCK_THREAD_NAME = "jvm.lock_thread_name";
  public static final String MESSAGE_REQUEST_PREFIX = "message.parameters.";
  public static final String REQUEST_REFERER_PARAMETER_NAME = "request.headers.referer";
  public static final String THREAD_NAME = "jvm.thread_name";
  public static final String SOLR_LUCENE_QUERY = "library.solr.lucene_query";
  public static final String SOLR_LUCENE_QUERY_STRING = "library.solr.lucene_query_string";
  public static final String SOLR_QUERY_STRING = "library.solr.query_string";
  public static final String SOLR_RAW_QUERY_STRING = "library.solr.raw_query_string";
  public static final String SOLR_DEBUG_INFO_ERROR = "library.solr.solr_debug_info_error";
  public static final String HTTP_REQUEST_STAR = "request.parameters.*";
  public static final String MESSAGE_REQUEST_STAR = "message.parameters.*";
  public static final String SOLR_STAR = "library.solr.*";
  public static final String JVM_STAR = "jvm.*";
  public static final String DISPLAY_HOST = "host.displayName";
  public static final String INSTANCE_NAME = "process.instanceName";
}