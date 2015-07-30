package com.newrelic.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.util.Streams;
import com.newrelic.deps.org.apache.commons.cli.CommandLine;

public class Deployments {
    static final String REVISION_OPTION = "revision";
    static final String CHANGE_LOG_OPTION = "changes";
    static final String APP_NAME_OPTION = "appname";
    static final String USER_OPTION = "user";
    static final String ENVIRONMENT_OPTION = "environment";

    static int recordDeployment(CommandLine cmd) throws Exception {
        if (cmd.hasOption("environment")) {
            System.setProperty("newrelic.environment", cmd.getOptionValue("environment"));
        }
        AgentConfig config = ConfigServiceFactory.createConfigService().getDefaultAgentConfig();
        return recordDeployment(cmd, config);
    }

    static int recordDeployment(CommandLine cmd, AgentConfig config) throws Exception {
        String appName = config.getApplicationName();
        if (cmd.hasOption("appname")) {
            appName = cmd.getOptionValue("appname");
        }
        if (appName == null) {
            throw new IllegalArgumentException("A deployment must be associated with an application.  Set app_name in"
                                                       + " newrelic.yml or specify the application name with the "
                                                       + "-appname switch.");
        }

        System.out.println("Recording a deployment for application " + appName);

        String uri = "/deployments.xml";
        String payload = getDeploymentPayload(appName, cmd);
        String protocol = "http" + (config.isSSL() ? "s" : "");
        URL url = new URL(protocol, config.getApiHost(), config.getApiPort(), uri);

        System.out.println(MessageFormat.format("Opening connection to {0}:{1}", new Object[] {config.getApiHost(),
                                                                                                      Integer.toString(config.getApiPort())}));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("x-license-key", config.getLicenseKey());

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        conn.setDoInput(true);

        conn.setRequestProperty("Content-Length", Integer.toString(payload.length()));
        conn.setFixedLengthStreamingMode(payload.length());
        conn.getOutputStream().write(payload.getBytes());

        int responseCode = conn.getResponseCode();
        if (responseCode < 300) {
            System.out.println("Deployment successfully recorded");
        } else if (responseCode == 401) {
            System.out.println("Unable to notify New Relic of the deployment because of an authorization error.  Check "
                                       + "your license key.");
            System.out.println("Response message: " + conn.getResponseMessage());
        } else {
            System.out.println("Unable to notify New Relic of the deployment");
            System.out.println("Response message: " + conn.getResponseMessage());
        }
        boolean isError = responseCode >= 300;
        if ((isError) || (config.isDebugEnabled())) {
            System.out.println("Response code: " + responseCode);
            InputStream inStream = isError ? conn.getErrorStream() : conn.getInputStream();

            if (inStream != null) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Streams.copy(inStream, output);

                PrintStream out = isError ? System.err : System.out;

                out.println(output);
            }
        }
        return responseCode;
    }

    private static String getDeploymentPayload(String appName, CommandLine cmd) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("deployment[timestamp]=").append(System.currentTimeMillis());

        builder.append("&deployment[appname]=").append(URLEncoder.encode(appName, "UTF-8"));
        if (cmd.getArgs().length > 1) {
            builder.append("&deployment[description]=").append(URLEncoder.encode(cmd.getArgs()[1], "UTF-8"));
        }
        if (cmd.hasOption("user")) {
            builder.append("&deployment[user]=").append(URLEncoder.encode(cmd.getOptionValue("user"), "UTF-8"));
        }
        if (cmd.hasOption("revision")) {
            builder.append("&deployment[revision]=").append(URLEncoder.encode(cmd.getOptionValue("revision"), "UTF-8"));
        }

        if (cmd.hasOption("changes")) {
            System.out.println("Reading the change log from standard input...");
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Streams.copy(System.in, output);

                builder.append("&deployment[changelog]=").append(URLEncoder.encode(output.toString(), "UTF-8"));
            } catch (IOException ex) {
                throw new IOException("An error occurred reading the change log from standard input", ex);
            }
        }

        return builder.toString();
    }
}