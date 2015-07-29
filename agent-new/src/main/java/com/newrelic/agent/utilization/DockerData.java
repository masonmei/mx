package com.newrelic.agent.utilization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;

public class DockerData {
    private static final String FILE_WITH_CONTAIER_ID = "/proc/self/cgroup";
    private static final String CPU = "cpu";
    private static final Pattern DOCKER_NATVIE_DRIVER_WOUT_SYSTEMD = Pattern.compile("^/docker/([0-9a-f]+)$");
    private static final Pattern DOCKER_NATIVE_DRIVER_W_SYSTEMD =
            Pattern.compile("^/system\\.slice/docker-([0-9a-f]+)\\.scope$");
    private static final Pattern DOCKER_LXC_DRVIER = Pattern.compile("^/lxc/([0-9a-f]+)$");

    public static String getDockerContainerId(boolean isLinux) {
        if (isLinux) {
            File cpuInfoFile = new File("/proc/self/cgroup");
            return getDockerIdFromFile(cpuInfoFile);
        }

        return null;
    }

    protected static String getDockerIdFromFile(File cpuInfoFile) {
        if ((cpuInfoFile.exists()) && (cpuInfoFile.canRead())) {
            try {
                FileReader fileReader = new FileReader(cpuInfoFile);
                return readFile(fileReader);
            } catch (FileNotFoundException e) {
            }
        }
        return null;
    }

    protected static String readFile(Reader reader) {
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(reader);
            StringBuilder resultGoesHere = new StringBuilder();
            String line;
            while ((line = bReader.readLine()) != null) {
                if (checkLineAndGetResult(line, resultGoesHere)) {
                    String value = resultGoesHere.toString().trim();
                    String str1;
                    if (isInvalidDockerValue(value)) {
                        Agent.LOG.log(Level.WARNING, MessageFormat.format("Failed to validate Docker value {0}",
                                                                                 new Object[] {value}));
                        recordDockerError();
                        return null;
                    }

                    return value;
                }
            }
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINEST, e, "Exception occured when reading docker file.", new Object[0]);
            recordDockerError();
        } finally {
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    protected static boolean isInvalidDockerValue(String value) {
        if (value == null) {
            return true;
        }

        if (value.length() != 64) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if ((c < '0') || (c > '9')) {
                if ((c < 'a') || (c > 'f')) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static void recordDockerError() {
        ServiceFactory.getStatsService()
                .doStatsWork(StatsWorks.getIncrementCounterWork("Supportability/utilization/docker/error", 1));
    }

    protected static boolean checkLineAndGetResult(String line, StringBuilder resultGoesHere) {
        String[] parts = line.split(":");
        if ((parts.length == 3) && (validCpuLine(parts[1]))) {
            String mayContainId = parts[2];
            if (checkAndGetMatch(DOCKER_NATVIE_DRIVER_WOUT_SYSTEMD, resultGoesHere, mayContainId)) {
                return true;
            }
            if (checkAndGetMatch(DOCKER_NATIVE_DRIVER_W_SYSTEMD, resultGoesHere, mayContainId)) {
                return true;
            }
            if (checkAndGetMatch(DOCKER_LXC_DRVIER, resultGoesHere, mayContainId)) {
                return true;
            }
            if (!mayContainId.equals("/")) {
                Agent.LOG.log(Level.FINE, "Docker Data: Ignoring unrecognized cgroup ID format: {0}",
                                     new Object[] {mayContainId});
            }
        }
        return false;
    }

    private static boolean validCpuLine(String segment) {
        if (segment != null) {
            String[] parts = segment.split(",");
            for (String current : parts) {
                if (current.equals("cpu")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkAndGetMatch(Pattern p, StringBuilder result, String segment) {
        Matcher m = p.matcher(segment);
        if ((m.matches()) && (m.groupCount() == 1)) {
            result.append(m.group(1));
            return true;
        }
        return false;
    }
}