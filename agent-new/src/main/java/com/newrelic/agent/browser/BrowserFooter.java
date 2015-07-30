package com.newrelic.agent.browser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BrowserMonitoringConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.deps.org.json.simple.JSONObject;

public class BrowserFooter {
    public static final String FOOTER_START_SCRIPT =
            "\n<script type=\"text/javascript\">window.NREUM||(NREUM={});NREUM.info=";
    public static final String FOOTER_END = "</script>";
    private static final String BEACON_KEY = "beacon";
    private static final String ERROR_BEACON_KEY = "errorBeacon";
    private static final String LICENSE_KEY = "licenseKey";
    private static final String APPLCATION_ID_KEY = "applicationID";
    private static final String TRANSACTION_NAME_KEY = "transactionName";
    private static final String QUEUE_TIME_KEY = "queueTime";
    private static final String APP_TIME_KEY = "applicationTime";
    private static final String TRAN_TRACE_GUID_KEY = "ttGuid";
    private static final String AGENT_TOKEN_KEY = "agentToken";
    private static final String ATTS_KEY = "atts";
    private static final String SSL_FOR_HTTP_KEY = "sslForHttp";
    private static final String AGENT_PAYLOAD_SCRIPT_KEY = "agent";
    private final String beacon;
    private final String browserKey;
    private final String errorBeacon;
    private final String payloadScript;
    private final String appId;
    private final Boolean isSslForHttp;

    public BrowserFooter(String appName, String pBeacon, String pBrowserKey, String pErrorBeacon, String pPayloadScript,
                         String pAppId) {
        beacon = pBeacon;
        browserKey = pBrowserKey;
        errorBeacon = pErrorBeacon;
        payloadScript = pPayloadScript;
        appId = pAppId;
        BrowserMonitoringConfig config =
                ServiceFactory.getConfigService().getAgentConfig(appName).getBrowserMonitoringConfig();
        if (config.isSslForHttpSet()) {
            isSslForHttp = config.isSslForHttp();
        } else {
            isSslForHttp = null;
        }
    }

    protected static Map<String, Object> getAttributes(BrowserTransactionState state) {
        Map<String, Object> atts;

        if (ServiceFactory.getAttributesService().isAttributesEnabledForBrowser(state.getAppName())) {
            Map userAtts = ServiceFactory.getAttributesService()
                                   .filterBrowserAttributes(state.getAppName(), state.getUserAttributes());

            Map agentAtts = ServiceFactory.getAttributesService()
                                    .filterBrowserAttributes(state.getAppName(), state.getAgentAttributes());

            atts = new HashMap<String, Object>(3);

            if ((!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity())
                        && (!userAtts.isEmpty())) {
                atts.put("u", userAtts);
            }
            if (!agentAtts.isEmpty()) {
                atts.put("a", agentAtts);
            }
        } else {
            atts = Collections.emptyMap();
        }
        return atts;
    }

    public String getFooter(BrowserTransactionState state) {
        String jsonString = jsonToString(createMapWithData(state));
        if (jsonString != null) {
            return "\n<script type=\"text/javascript\">window.NREUM||(NREUM={});NREUM.info=" + jsonString + "</script>";
        }
        return "";
    }

    private String jsonToString(Map<String, ? extends Object> map) {
        ByteArrayOutputStream baos = null;
        Writer out = null;
        try {
            baos = new ByteArrayOutputStream();
            out = new OutputStreamWriter(baos, "UTF-8");
            JSONObject.writeJSONString(map, out);
            out.flush();
            return new String(baos.toByteArray());
        } catch (Exception e) {
            Agent.LOG.log(Level.INFO, "An error occured when creating the rum footer. Issue:" + e.getMessage());
            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.log(Level.FINEST, "Exception when creating rum footer. ", e);
            }
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private Map<String, Object> createMapWithData(BrowserTransactionState state) {
        Map<String, Object> output = new HashMap<String, Object>();

        output.put("beacon", beacon);
        output.put("errorBeacon", errorBeacon);
        output.put("licenseKey", browserKey);
        output.put("applicationID", appId);
        output.put("agent", payloadScript);

        output.put("queueTime", state.getExternalTimeInMilliseconds());
        output.put("applicationTime", state.getDurationInMilliseconds());
        output.put("transactionName", obfuscate(state.getTransactionName()));

        addToMapIfNotNullOrEmpty(output, "sslForHttp", isSslForHttp);

        addToMapIfNotNullAndObfuscate(output, "atts", getAttributes(state));
        return output;
    }

    private void addToMapIfNotNullOrEmpty(Map<String, Object> map, String key, String value) {
        if ((value != null) && (!value.isEmpty())) {
            map.put(key, value);
        }
    }

    private void addToMapIfNotNullOrEmpty(Map<String, Object> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private void addToMapIfNotNullAndObfuscate(Map<String, Object> map, String key,
                                               Map<String, ? extends Object> value) {
        if ((value != null) && (!value.isEmpty())) {
            String output = jsonToString(value);
            if ((output != null) && (!output.isEmpty())) {
                map.put(key, obfuscate(output));
            }
        }
    }

    private String obfuscate(String name) {
        if ((name == null) || (name.length() == 0)) {
            return "";
        }
        String licenseKey = ServiceFactory.getConfigService().getDefaultAgentConfig().getLicenseKey();
        try {
            return Obfuscator.obfuscateNameUsingKey(name, licenseKey.substring(0, 13));
        } catch (UnsupportedEncodingException e) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Error obfuscating {0}: {1}", name, e);
                Agent.LOG.finer(msg);
            }
        }
        return "";
    }
}