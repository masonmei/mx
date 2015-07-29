package com.newrelic.agent.utilization;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.deps.org.apache.http.client.config.RequestConfig;
import com.newrelic.deps.org.apache.http.client.methods.CloseableHttpResponse;
import com.newrelic.deps.org.apache.http.client.methods.HttpGet;
import com.newrelic.deps.org.apache.http.config.SocketConfig;
import com.newrelic.deps.org.apache.http.conn.ConnectTimeoutException;
import com.newrelic.deps.org.apache.http.conn.ssl.StrictHostnameVerifier;
import com.newrelic.deps.org.apache.http.impl.client.CloseableHttpClient;
import com.newrelic.deps.org.apache.http.impl.client.HttpClientBuilder;
import com.newrelic.deps.org.apache.http.util.EntityUtils;

public class AWS {
    protected static final String INSTANCE_TYPE_URL = "http://169.254.169.254/2008-02-01/meta-data/instance-type";
    protected static final String INSTANCE_ID_URL = "http://169.254.169.254/2008-02-01/meta-data/instance-id";
    protected static final String INSTANCE_AVAILABLITY_ZONE =
            "http://169.254.169.254/2008-02-01/meta-data/placement/availability-zone";
    private static final int MIN_CHAR_CODEPOINT = "".codePointAt(0);
    private static int requestTimeoutInMillis = 100;

    private static CloseableHttpClient configureHttpClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(requestTimeoutInMillis).setSoKeepAlive(true)
                                               .build());
        RequestConfig.Builder requestBuilder = RequestConfig.custom().setConnectTimeout(requestTimeoutInMillis)
                                                       .setConnectionRequestTimeout(requestTimeoutInMillis)
                                                       .setSocketTimeout(requestTimeoutInMillis);

        builder.setDefaultRequestConfig(requestBuilder.build());
        builder.setHostnameVerifier(new StrictHostnameVerifier());
        return builder.build();
    }

    private static void recordAwsError() {
        ServiceFactory.getStatsService()
                .doStatsWork(StatsWorks.getIncrementCounterWork("Supportability/utilization/aws/error", 1));
    }

    protected AwsData getAwsData() {
        String type = getAwsValue("http://169.254.169.254/2008-02-01/meta-data/instance-type");
        String id = type == null ? null : getAwsValue("http://169.254.169.254/2008-02-01/meta-data/instance-id");
        String zone = (type == null) && (id == null) ? null
                              : getAwsValue("http://169.254.169.254/2008-02-01/meta-data/placement/availability-zone");

        if ((type == null) || (id == null) || (zone == null)) {
            return AwsData.EMPTY_DATA;
        }

        return new AwsData(id, type, zone);
    }

    protected String getAwsValue(String url) {
        try {
            String value = makeHttpRequest(url);
            if (isInvalidAwsValue(value)) {
                Agent.LOG.log(Level.WARNING,
                                     MessageFormat.format("Failed to validate AWS value {0}", new Object[] {value}));
                recordAwsError();
                return null;
            }

            return value.trim();
        } catch (ConnectTimeoutException e) {
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST,
                                 MessageFormat.format("Error occurred trying to get AWS value. {0}", new Object[] {t}));
            recordAwsError();
        }
        return null;
    }

    protected boolean isInvalidAwsValue(String value) {
        if (value == null) {
            return true;
        }

        if (value.getBytes().length > 255) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if ((c < '0') || (c > '9')) {
                if ((c < 'a') || (c > 'z')) {
                    if ((c < 'A') || (c > 'Z')) {
                        if ((c != ' ') && (c != '_') && (c != '.') && (c != '/') && (c != '-')) {
                            if (c <= MIN_CHAR_CODEPOINT) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    protected String makeHttpRequest(String url) throws IOException {
        CloseableHttpClient httpclient = null;
        try {
            httpclient = configureHttpClient();
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpclient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() <= 207) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } finally {
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    protected static class AwsData {
        static final AwsData EMPTY_DATA = new AwsData();
        private final String instanceId;
        private final String instanceType;
        private final String availablityZone;

        private AwsData() {
            instanceId = null;
            instanceType = null;
            availablityZone = null;
        }

        protected AwsData(String id, String type, String zone) {
            instanceId = id;
            instanceType = type;
            availablityZone = zone;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getInstanceType() {
            return instanceType;
        }

        public String getAvailabityZone() {
            return availablityZone;
        }
    }
}