//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.transport;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.UnexpectedException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;

import com.newrelic.deps.org.apache.http.Header;
import com.newrelic.deps.org.apache.http.HttpEntity;
import com.newrelic.deps.org.apache.http.HttpHost;
import com.newrelic.deps.org.apache.http.StatusLine;
import com.newrelic.deps.org.apache.http.auth.AuthScope;
import com.newrelic.deps.org.apache.http.auth.Credentials;
import com.newrelic.deps.org.apache.http.auth.UsernamePasswordCredentials;
import com.newrelic.deps.org.apache.http.client.config.RequestConfig;
import com.newrelic.deps.org.apache.http.client.config.RequestConfig.Builder;
import com.newrelic.deps.org.apache.http.client.methods.CloseableHttpResponse;
import com.newrelic.deps.org.apache.http.client.methods.HttpUriRequest;
import com.newrelic.deps.org.apache.http.client.methods.RequestBuilder;
import com.newrelic.deps.org.apache.http.client.protocol.HttpClientContext;
import com.newrelic.deps.org.apache.http.config.SocketConfig;
import com.newrelic.deps.org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import com.newrelic.deps.org.apache.http.conn.ssl.SSLContextBuilder;
import com.newrelic.deps.org.apache.http.conn.ssl.StrictHostnameVerifier;
import com.newrelic.deps.org.apache.http.entity.ByteArrayEntity;
import com.newrelic.deps.org.apache.http.impl.client.BasicCredentialsProvider;
import com.newrelic.deps.org.apache.http.impl.client.CloseableHttpClient;
import com.newrelic.deps.org.apache.http.impl.client.HttpClientBuilder;
import com.newrelic.deps.org.apache.http.message.BasicHeader;
import com.newrelic.deps.org.apache.http.protocol.HttpContext;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import com.newrelic.deps.org.json.simple.JSONValue;
import com.newrelic.deps.org.json.simple.parser.JSONParser;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ForceDisconnectException;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.MetricDataException;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.errors.TracedError;
import com.newrelic.agent.logging.ApacheCommonsAdaptingLogFactory;
import com.newrelic.agent.profile.IProfile;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.analytics.CustomInsightsEvent;
import com.newrelic.agent.service.analytics.TransactionEvent;
import com.newrelic.agent.service.module.Jar;
import com.newrelic.agent.sql.SqlTrace;
import com.newrelic.agent.trace.TransactionTrace;
import com.newrelic.agent.util.RubyConversion;

public class DataSenderImpl implements DataSender {
    private static final String MODULE_TYPE = "Jars";
    private static final int PROTOCOL_VERSION = 14;
    private static final int DEFAULT_REQUEST_TIMEOUT_IN_SECONDS = 120;
    private static final String BEFORE_LICENSE_KEY_URI_PATTERN = "/agent_listener/invoke_raw_method?method={0}";
    private static final String AFTER_LICENSE_KEY_URI_PATTERN = "&marshal_format=json&protocol_version=14";
    private static final String LICENSE_KEY_URI_PATTERN = "&license_key={0}";
    private static final String RUN_ID_PATTERN = "&run_id={1}";
    private static final String CONNECT_METHOD = "connect";
    private static final String METRIC_DATA_METHOD = "metric_data";
    private static final String GET_AGENT_COMMANDS_METHOD = "get_agent_commands";
    private static final String AGENT_COMMAND_RESULTS_METHOD = "agent_command_results";
    private static final String GET_REDIRECT_HOST_METHOD = "get_redirect_host";
    private static final String ERROR_DATA_METHOD = "error_data";
    private static final String PROFILE_DATA_METHOD = "profile_data";
    private static final String QUEUE_PING_COMMAND_METHOD = "queue_ping_command";
    private static final String ANALYTIC_DATA_METHOD = "analytic_event_data";
    private static final String CUSTOM_ANALYTIC_DATA_METHOD = "custom_event_data";
    private static final String UPDATE_LOADED_MODULES_METHOD = "update_loaded_modules";
    private static final String SHUTDOWN_METHOD = "shutdown";
    private static final String SQL_TRACE_DATA_METHOD = "sql_trace_data";
    private static final String TRANSACTION_SAMPLE_DATA_METHOD = "transaction_sample_data";
    private static final String USER_AGENT_HEADER_VALUE = initUserHeaderValue();
    private static final String GZIP = "gzip";
    private static final String DEFLATE_ENCODING = "deflate";
    private static final String IDENTITY_ENCODING = "identity";
    private static final String RESPONSE_MAP_EXCEPTION_KEY = "exception";
    private static final String EXCEPTION_MAP_MESSAGE_KEY = "message";
    private static final String EXCEPTION_MAP_ERROR_TYPE_KEY = "error_type";
    private static final String EXCEPTION_MAP_RETURN_VALUE_KEY = "return_value";
    private static final String AGENT_RUN_ID_KEY = "agent_run_id";
    private static final String SSL_KEY = "ssl";
    private static final Object NO_AGENT_RUN_ID = null;
    private static final String NULL_RESPONSE = "null";
    private static final String TIMEOUT_PROPERTY = "timeout";
    private static final int COMPRESSION_LEVEL = -1;
    private static final String GET_XRAY_PARMS_METHOD = "get_xray_metadata";
    private final int port;
    private final HttpHost proxy;
    private final Credentials proxyCredentials;
    private final int defaultTimeoutInMillis;
    private final String agentRunIdUriPattern;
    private final String noAgentRunIdUriPattern;
    private final boolean usePrivateSSL;
    private final boolean useSSL;
    private final SSLContext sslContext;
    private volatile String host;
    private volatile String protocol;
    private volatile boolean auditMode;
    private volatile Object agentRunId;

    public DataSenderImpl(AgentConfig config) {
        this.agentRunId = NO_AGENT_RUN_ID;
        System.setProperty("com.newrelic.deps.org.apache.commons.logging.LogFactory", ApacheCommonsAdaptingLogFactory.class.getName());
        this.auditMode = config.isAuditMode();
        Agent.LOG.info(MessageFormat
                               .format("Setting audit_mode to {0}", new Object[] {Boolean.valueOf(this.auditMode)}));
        this.host = config.getHost();
        this.port = config.getPort();
        this.useSSL = config.isSSL();
        this.protocol = this.useSSL ? "https" : "http";
        String msg = MessageFormat.format("Setting protocol to \"{0}\"", new Object[] {this.protocol});
        Agent.LOG.info(msg);
        String proxyHost = config.getProxyHost();
        Integer proxyPort = config.getProxyPort();
        this.usePrivateSSL = config.isUsePrivateSSL();
        this.sslContext = this.createSSLContext();
        if (proxyHost != null && proxyPort != null) {
            msg = MessageFormat.format("Using proxy host {0}:{1}",
                                              new Object[] {proxyHost, Integer.toString(proxyPort.intValue())});
            Agent.LOG.fine(msg);
            this.proxy = new HttpHost(proxyHost, proxyPort.intValue());
            this.proxyCredentials =
                    this.getProxyCredentials(proxyHost, proxyPort, config.getProxyUser(), config.getProxyPassword());
        } else {
            this.proxy = null;
            this.proxyCredentials = null;
        }

        this.defaultTimeoutInMillis = ((Integer) config.getProperty("timeout", Integer.valueOf(120))).intValue() * 1000;
        String licenseKeyUri = MessageFormat.format("&license_key={0}", new Object[] {config.getLicenseKey()});
        this.noAgentRunIdUriPattern = "/agent_listener/invoke_raw_method?method={0}" + licenseKeyUri
                                              + "&marshal_format=json&protocol_version=14";
        this.agentRunIdUriPattern = this.noAgentRunIdUriPattern + "&run_id={1}";
    }

    private static String initUserHeaderValue() {
        String arch = "unknown";
        String javaVersion = "unknown";

        try {
            arch = System.getProperty("os.arch");
            javaVersion = System.getProperty("java.version");
        } catch (Exception var3) {
            ;
        }

        return MessageFormat.format("NewRelic-JavaAgent/{0} (java {1} {2})",
                                           new Object[] {Agent.getVersion(), javaVersion, arch});
    }

    public static KeyStore getKeyStore()
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream in = DataSenderImpl.class.getResourceAsStream("/nrcerts");
        if (null == in) {
            Agent.LOG.fine("Unable to find NR trust store");
        } else {
            try {
                keystore.load(in, (char[]) null);
            } finally {
                in.close();
            }
        }

        Agent.LOG.finer("SSL Keystore Provider: " + keystore.getProvider().getName());
        return keystore;
    }

    private SSLContext createSSLContext() {
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();

        try {
            if (this.usePrivateSSL && this.useSSL) {
                sslContextBuilder.loadTrustMaterial(getKeyStore());
            }

            return sslContextBuilder.build();
        } catch (Exception var3) {
            return null;
        }
    }

    private Credentials getProxyCredentials(String proxyHost, Integer proxyPort, String proxyUser, String proxyPass) {
        if (proxyUser != null && proxyPass != null) {
            Agent.LOG.info(MessageFormat
                                   .format("Setting Proxy Authenticator for user \'{0}\'", new Object[] {proxyUser}));
            return new UsernamePasswordCredentials(proxyUser, proxyPass);
        } else {
            return null;
        }
    }

    private void checkAuditMode() {
        boolean auditMode2 = ServiceFactory.getConfigService().getLocalAgentConfig().isAuditMode();
        if (this.auditMode != auditMode2) {
            this.auditMode = auditMode2;
            Agent.LOG.info(MessageFormat.format("Setting audit_mode to {0}",
                                                       new Object[] {Boolean.valueOf(this.auditMode)}));
        }

    }

    private void setAgentRunId(Object runId) {
        this.agentRunId = runId;
        if (runId != NO_AGENT_RUN_ID) {
            Agent.LOG.info("Agent run id: " + runId);
        }

    }

    public Map<String, Object> connect(Map<String, Object> startupOptions) throws Exception {
        String redirectHost = this.getRedirectHost();
        if (redirectHost != null) {
            this.host = redirectHost;
            String msg = MessageFormat.format("Collector redirection to {0}:{1}",
                                                     new Object[] {this.host, Integer.toString(this.port)});
            Agent.LOG.info(msg);
        }

        return this.doConnect(startupOptions);
    }

    private String getRedirectHost() throws Exception {
        Object response = this.invokeNoRunId("get_redirect_host", "deflate", new InitialSizedJsonArray(0));
        return response == null ? null : response.toString();
    }

    private Map<String, Object> doConnect(Map<String, Object> startupOptions) throws Exception {
        InitialSizedJsonArray params = new InitialSizedJsonArray(1);
        params.add(startupOptions);
        Object response = this.invokeNoRunId("connect", "deflate", params);
        if (!(response instanceof Map)) {
            String data1 = MessageFormat.format("Expected a map of connection data, got {0}", new Object[] {response});
            throw new UnexpectedException(data1);
        } else {
            Map data = (Map) response;
            if (data.containsKey("agent_run_id")) {
                Object ssl1 = data.get("agent_run_id");
                this.setAgentRunId(ssl1);
                ssl1 = data.get("ssl");
                if (Boolean.TRUE.equals(ssl1)) {
                    Agent.LOG.info("Setting protocol to \"https\"");
                    this.protocol = "https";
                }

                return data;
            } else {
                String ssl = MessageFormat.format("Missing {0} connection parameter", new Object[] {"agent_run_id"});
                throw new UnexpectedException(ssl);
            }
        }
    }

    public List<List<?>> getAgentCommands() throws Exception {
        this.checkAuditMode();
        Object runId = this.agentRunId;
        if (runId == NO_AGENT_RUN_ID) {
            return Collections.emptyList();
        } else {
            InitialSizedJsonArray params = new InitialSizedJsonArray(1);
            params.add(runId);
            Object response = this.invokeRunId("get_agent_commands", "deflate", runId, params);
            if (response != null && !"null".equals(response)) {
                try {
                    return (List) response;
                } catch (ClassCastException var6) {
                    String msg = MessageFormat
                                         .format("Invalid response from New Relic when getting agent commands: {0}",
                                                        new Object[] {var6});
                    Agent.LOG.warning(msg);
                    throw var6;
                }
            } else {
                return Collections.emptyList();
            }
        }
    }

    public List<?> getXRayParameters(Collection<Long> ids) throws Exception {
        if (ids.size() <= 0) {
            Agent.LOG.info("Attempted to fetch X-Ray Session metadata with no session IDs");
            return Collections.emptyList();
        } else {
            this.checkAuditMode();
            Object runId = this.agentRunId;
            if (runId == NO_AGENT_RUN_ID) {
                return Collections.emptyList();
            } else {
                JSONArray params = new JSONArray();
                params.add(runId);
                Iterator response = ids.iterator();

                while (response.hasNext()) {
                    Long e = (Long) response.next();
                    params.add(e);
                }

                Object response1 = this.invokeRunId("get_xray_metadata", "deflate", runId, params);
                if (response1 != null && !"null".equals(response1)) {
                    try {
                        return (List) response1;
                    } catch (ClassCastException var7) {
                        String msg = MessageFormat
                                             .format("Invalid response from New Relic when getting agent X Ray "
                                                             + "parameters: {0}",
                                                            new Object[] {var7});
                        Agent.LOG.warning(msg);
                        throw var7;
                    }
                } else {
                    return Collections.emptyList();
                }
            }
        }
    }

    public void queuePingCommand() throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(1);
            params.add(runId);
            this.invokeRunId("queue_ping_command", "deflate", runId, params);
        }
    }

    public void sendCommandResults(Map<Long, Object> commandResults) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !commandResults.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add(runId);
            params.add(commandResults);
            this.invokeRunId("agent_command_results", "deflate", runId, params);
        }
    }

    public void sendErrorData(List<TracedError> errors) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !errors.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add(runId);
            params.add(errors);
            this.invokeRunId("error_data", "identity", runId, params);
        }
    }

    public void sendAnalyticsEvents(Collection<TransactionEvent> events) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !events.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add(runId);
            params.add(events);
            this.invokeRunId("analytic_event_data", "deflate", runId, params);
        }
    }

    public void sendCustomAnalyticsEvents(Collection<CustomInsightsEvent> events) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !events.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add(runId);
            params.add(events);
            this.invokeRunId("custom_event_data", "deflate", runId, params);
        }
    }

    public List<List<?>> sendMetricData(long beginTimeMillis, long endTimeMillis, List<MetricData> metricData)
            throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !metricData.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(4);
            params.add(runId);
            params.add(Long.valueOf(beginTimeMillis / 1000L));
            params.add(Long.valueOf(endTimeMillis / 1000L));
            params.add(metricData);
            Object response = this.invokeRunId("metric_data", "deflate", runId, params);
            if (response != null && !"null".equals(response)) {
                try {
                    return (List) response;
                } catch (ClassCastException var11) {
                    String msg = MessageFormat.format("Invalid response from New Relic when sending metric data: {0}",
                                                             new Object[] {var11});
                    Agent.LOG.warning(msg);
                    throw var11;
                }
            } else {
                throw new MetricDataException("Invalid null response sending metric data");
            }
        } else {
            return Collections.emptyList();
        }
    }

    public List<Long> sendProfileData(List<IProfile> profiles) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !profiles.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add(runId);
            params.add(profiles);
            Object response = this.invokeRunId("profile_data", "identity", runId, params);
            if (response != null && !"null".equals(response)) {
                try {
                    return (List) response;
                } catch (ClassCastException var7) {
                    String msg = MessageFormat.format("Invalid response from New Relic sending profiles: {0}",
                                                             new Object[] {var7});
                    Agent.LOG.warning(msg);
                    throw var7;
                }
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }

    public void sendModules(List<Jar> pJars) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && pJars != null && !pJars.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add("Jars");
            params.add(pJars);
            Object response = this.invokeRunId("update_loaded_modules", "identity", runId, params);
            if (response != null && !"null".equals(response)) {
                String msg = MessageFormat.format("Invalid response from New Relic when sending modules. Response: {0}",
                                                         new Object[] {response});
                Agent.LOG.warning(msg);
            }
        }
    }

    public void sendSqlTraceData(List<SqlTrace> sqlTraces) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !sqlTraces.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(1);
            params.add(sqlTraces);
            Object response = this.invokeRunId("sql_trace_data", "identity", runId, params);
            if (response != null && !"null".equals(response)) {
                String msg = MessageFormat
                                     .format("Invalid response from New Relic when sending sql traces. Response: {0}",
                                                    new Object[] {response});
                Agent.LOG.warning(msg);
            }
        }
    }

    public void sendTransactionTraceData(List<TransactionTrace> traces) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID && !traces.isEmpty()) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add(runId);
            params.add(traces);
            Object response = this.invokeRunId("transaction_sample_data", "identity", runId, params);
            if (response != null && !"null".equals(response)) {
                String msg = MessageFormat
                                     .format("Invalid response from New Relic when sending transaction traces. "
                                                     + "Response: {0}",
                                                    new Object[] {response});
                Agent.LOG.warning(msg);
            }
        }
    }

    public void shutdown(long timeMillis) throws Exception {
        Object runId = this.agentRunId;
        if (runId != NO_AGENT_RUN_ID) {
            InitialSizedJsonArray params = new InitialSizedJsonArray(2);
            params.add(runId);
            params.add(Long.valueOf(timeMillis));
            short requestTimeoutInMillis = 10000;

            try {
                this.invokeRunId("shutdown", "deflate", runId, requestTimeoutInMillis, params);
            } finally {
                this.setAgentRunId(NO_AGENT_RUN_ID);
            }

        }
    }

    private Object invokeRunId(String method, String encoding, Object runId, JSONStreamAware params) throws Exception {
        return this.invokeRunId(method, encoding, runId, this.defaultTimeoutInMillis, params);
    }

    private Object invokeRunId(String method, String encoding, Object runId, int timeoutInMillis,
                               JSONStreamAware params) throws Exception {
        String uri = MessageFormat.format(this.agentRunIdUriPattern, new Object[] {method, runId.toString()});
        return this.invoke(method, encoding, uri, params, timeoutInMillis);
    }

    private Object invokeNoRunId(String method, String encoding, JSONStreamAware params) throws Exception {
        String uri = MessageFormat.format(this.noAgentRunIdUriPattern, new Object[] {method});
        return this.invoke(method, encoding, uri, params, this.defaultTimeoutInMillis);
    }

    private Object invoke(String method, String encoding, String uri, JSONStreamAware params, int timeoutInMillis)
            throws Exception {
        ReadResult readResult = this.send(method, encoding, uri, params, timeoutInMillis);
        Map responseMap = null;
        String responseBody = readResult.getResponseBody();
        if (responseBody != null) {
            Exception ex = null;

            try {
                responseMap = this.getResponseMap(responseBody);
                ex = this.parseException(responseMap);
            } catch (Exception var11) {
                Agent.LOG.log(Level.WARNING, "Error parsing response JSON({0}) from NewRelic: {1}",
                                     new Object[] {method, var11.toString()});
                Agent.LOG.log(Level.FINEST, "Invalid response JSON({0}): {1}", new Object[] {method, responseBody});
                throw var11;
            }

            if (ex != null) {
                throw ex;
            }
        } else {
            Agent.LOG.log(Level.FINER, "Response was null ({0})", new Object[] {method});
        }

        return responseMap != null ? responseMap.get("return_value") : null;
    }

    private ReadResult connectAndSend(String method, String encoding, String uri, JSONStreamAware params,
                                      int timeoutInMillis) throws Exception {
        CloseableHttpClient conn = null;

        ReadResult msg1;
        try {
            conn = this.createHttpClient(encoding, uri, timeoutInMillis);
            byte[] data = this.writeData(encoding, params);
            HttpUriRequest request = this.createRequest(encoding, uri, timeoutInMillis, data);
            HttpContext context = this.createHttpContext();
            CloseableHttpResponse response = conn.execute(request, context);

            try {
                StatusLine statusLine = response.getStatusLine();
                if (statusLine == null) {
                    throw new Exception("The http response has no status line");
                }

                String responseBody;
                if (this.auditMode) {
                    responseBody = MessageFormat.format("Sent JSON({0}) to: {1}\n{2}",
                                                               new Object[] {method, request.getURI(), DataSenderWriter
                                                                                                               .toJSONString(params)});
                    Agent.LOG.info(responseBody);
                }

                int statusCode = statusLine.getStatusCode();
                if (statusCode == 407) {
                    responseBody = response.getFirstHeader("Proxy-Authenticate").getValue();
                    throw new HttpError("Proxy Authentication Mechanism Failed: " + responseBody, statusCode);
                }

                if (statusCode != 200) {
                    Agent.LOG.log(Level.FINER, "Connection http status code: {0}",
                                         new Object[] {Integer.valueOf(statusCode)});
                    throw HttpError.create(statusCode, this.host);
                }

                responseBody = this.readResponseBody(response);
                if (this.auditMode) {
                    String msg = MessageFormat.format("Received JSON({0}): {1}", new Object[] {method, responseBody});
                    Agent.LOG.info(msg);
                }

                msg1 = ReadResult.create(statusCode, responseBody);
            } finally {
                response.close();
            }
        } finally {
            if (conn != null) {
                conn.close();
            }

        }

        return msg1;
    }

    private HttpContext createHttpContext() {
        HttpClientContext context = new HttpClientContext();
        if (this.proxy != null && this.proxyCredentials != null) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(this.proxy), this.proxyCredentials);
            context.setCredentialsProvider(credentialsProvider);
        }

        return context;
    }

    private ReadResult send(String method, String encoding, String uri, JSONStreamAware params, int timeoutInMillis)
            throws Exception {
        try {
            return this.connectAndSend(method, encoding, uri, params, timeoutInMillis);
        } catch (MalformedURLException var8) {
            Agent.LOG.log(Level.SEVERE,
                                 "You have requested a connection to New Relic via a protocol which is unavailable in"
                                         + " your runtime: {0}",
                                 new Object[] {var8.toString()});
            throw new ForceDisconnectException(var8.toString());
        } catch (SocketException var9) {
            if (var9.getCause() instanceof NoSuchAlgorithmException) {
                String msg = MessageFormat.format("You have requested a connection to New Relic via an algorithm "
                                                          + "which is unavailable in your runtime: {0}  This may also"
                                                          + " be indicative of a corrupted keystore or trust store on"
                                                          + " your server.",
                                                         new Object[] {var9.getCause().toString()});
                Agent.LOG.error(msg);
            } else {
                Agent.LOG.log(Level.INFO,
                                     "A socket exception was encountered while sending data to New Relic ({0}).  "
                                             + "Please check your network / proxy settings.",
                                     new Object[] {var9.toString()});
                if (Agent.LOG.isLoggable(Level.FINE)) {
                    Agent.LOG.log(Level.FINE, "Error sending JSON({0}): {1}",
                                         new Object[] {method, DataSenderWriter.toJSONString(params)});
                }

                Agent.LOG.log(Level.FINEST, var9, var9.toString(), new Object[0]);
            }

            throw var9;
        } catch (HttpError var10) {
            throw var10;
        } catch (Exception var11) {
            Agent.LOG.log(Level.INFO, "Remote {0} call failed : {1}.", new Object[] {method, var11.toString()});
            if (Agent.LOG.isLoggable(Level.FINE)) {
                Agent.LOG.log(Level.FINE, "Error sending JSON({0}): {1}",
                                     new Object[] {method, DataSenderWriter.toJSONString(params)});
            }

            Agent.LOG.log(Level.FINEST, var11, var11.toString(), new Object[0]);
            throw var11;
        }
    }

    private HttpUriRequest createRequest(String encoding, String uri, int requestTimeoutInMillis, byte[] data)
            throws MalformedURLException, URISyntaxException {
        URL url = new URL(this.protocol, this.host, this.port, uri);
        RequestConfig config = RequestConfig.custom().setConnectTimeout(requestTimeoutInMillis)
                                       .setConnectionRequestTimeout(requestTimeoutInMillis)
                                       .setSocketTimeout(requestTimeoutInMillis).build();
        RequestBuilder requestBuilder = RequestBuilder.post().setUri(url.toURI()).setEntity(new ByteArrayEntity(data));
        requestBuilder.setConfig(config);
        return requestBuilder.build();
    }

    private CloseableHttpClient createHttpClient(String encoding, String uri, int requestTimeoutInMillis)
            throws Exception {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setUserAgent(USER_AGENT_HEADER_VALUE)
                .setDefaultHeaders(Arrays.asList(new Header[] {new BasicHeader("Connection", "Keep-Alive"),
                                                                      new BasicHeader("CONTENT-TYPE",
                                                                                             "application/octet-stream"),
                                                                      new BasicHeader("ACCEPT-ENCODING", "gzip"),
                                                                      new BasicHeader("CONTENT-ENCODING", encoding)}));
        builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(requestTimeoutInMillis).setSoKeepAlive(true)
                                               .build());
        Builder requestBuilder = RequestConfig.custom().setConnectTimeout(requestTimeoutInMillis)
                                         .setConnectionRequestTimeout(requestTimeoutInMillis)
                                         .setSocketTimeout(requestTimeoutInMillis);
        builder.setDefaultRequestConfig(requestBuilder.build());
        builder.setHostnameVerifier(new StrictHostnameVerifier());
        if (this.proxy != null) {
            builder.setProxy(this.proxy);
        }

        if (this.sslContext != null) {
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(this.sslContext));
        }

        CloseableHttpClient httpClient = builder.build();
        return httpClient;
    }

    private byte[] writeData(String encoding, JSONStreamAware params) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        OutputStreamWriter out = null;

        try {
            OutputStream os = this.getOutputStream(outStream, encoding);
            out = new OutputStreamWriter(os, "UTF-8");
            JSONValue.writeJSONString(params, out);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }

        }

        return outStream.toByteArray();
    }

    private OutputStream getOutputStream(OutputStream out, String encoding) throws IOException {
        return (OutputStream) ("deflate".equals(encoding) ? new DeflaterOutputStream(out, new Deflater(-1)) : out);
    }

    private String readResponseBody(CloseableHttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new Exception("The http response entity was null");
        } else {
            InputStream is = entity.getContent();
            BufferedReader in = this.getBufferedReader(response, is);

            String var5;
            try {
                var5 = in.readLine();
            } finally {
                in.close();
                is.close();
            }

            return var5;
        }
    }

    private Map<?, ?> getResponseMap(String responseBody) throws Exception {
        JSONParser parser = new JSONParser();
        Object response = parser.parse(responseBody);
        return (Map) Map.class.cast(response);
    }

    private BufferedReader getBufferedReader(CloseableHttpResponse response, InputStream is) throws IOException {
        Header encodingHeader = response.getFirstHeader("content-encoding");
        if (encodingHeader != null) {
            String encoding = encodingHeader.getValue();
            if ("gzip".equals(encoding)) {
                is = new GZIPInputStream((InputStream) is);
            }
        }

        return new BufferedReader(new InputStreamReader((InputStream) is, "UTF-8"));
    }

    private Exception parseException(Map<?, ?> responseMap) throws Exception {
        Object exception = responseMap.get("exception");
        if (exception == null) {
            return null;
        } else {
            Map exceptionMap = (Map) Map.class.cast(exception);

            String message;
            try {
                message = (String) exceptionMap.get("message");
            } catch (Exception var8) {
                message = exceptionMap.toString();
            }

            String type = (String) exceptionMap.get("error_type");
            Class clazz = RubyConversion.rubyClassToJavaClass(type);
            Constructor constructor = clazz.getConstructor(new Class[] {String.class});
            return (Exception) constructor.newInstance(new Object[] {message});
        }
    }
}
