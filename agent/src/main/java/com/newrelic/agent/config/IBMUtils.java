package com.newrelic.agent.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IBMUtils {
    private static final Pattern srNumberPattern = Pattern.compile("\\(SR([0-9]+)[^()]*\\)\\s*$");

    public static boolean getIbmWorkaroundDefault() {
        try {
            String jvmVendor = System.getProperty("java.vendor");
            if ("IBM Corporation".equals(jvmVendor)) {
                String jvmVersion = System.getProperty("java.specification.version", "");
                int srNum = getIbmSRNumber();

                if (("1.6".equals(jvmVersion)) && (srNum >= 4)) {
                    return false;
                }
                if (("1.7".equals(jvmVersion)) && (srNum >= 4)) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (Exception e) {
        }
        return true;
    }

    public static int getIbmSRNumber() {
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            String runtimeVersion = System.getProperty("java.runtime.version", "");
            Matcher matcher = srNumberPattern.matcher(runtimeVersion);
            if (matcher.find()) {
                return Integer.valueOf(matcher.group(1)).intValue();
            }
        }
        return -1;
    }
}