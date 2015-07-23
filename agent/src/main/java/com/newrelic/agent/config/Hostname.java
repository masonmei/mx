package com.newrelic.agent.config;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;

public class Hostname {
    public static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }
        return "localhost";
    }

    public static String getDisplayHostname(IAgentLogger log, AgentConfig config, String defaultHostname,
                                            String appName) {
        String specifiedHost = (String) config.getValue("process_host.display_name", defaultHostname);
        log.log(Level.INFO, "Display host name is {0} for application {1}", new Object[] {specifiedHost, appName});
        return specifiedHost;
    }

    public static String getHostname(IAgentLogger log, AgentConfig config) {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            Agent.LOG.log(Level.FINE, "Error getting host name", e);
            try {
                InetAddress inetAddress = getInetAddress(config);
                if (inetAddress == null) {
                    Agent.LOG.severe("Unable to obtain a host name for this JVM, defaulting to localhost."
                                             + getMessage());

                    return "localhost";
                }
                Agent.LOG.severe("Unable to obtain a host name for this JVM.  Using IP address." + getMessage());
                return inetAddress.getHostAddress();
            } catch (Exception err) {
                Agent.LOG.log(Level.FINE, "Error getting IP address", err);
            }
        }
        return "localhost";
    }

    private static String getMessage() {
        String osName = ManagementFactory.getOperatingSystemMXBean().getName();
        if (("Linux".equals(osName)) || ("Mac OS X".equals(osName))) {
            return "  You might need to add a host entry for this machine in /etc/hosts";
        }
        return "";
    }

    private static List<NetworkInterface> getNetworkInterfaces() {
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
        }
        return Collections.emptyList();
    }

    protected static InetAddress getInetAddress(AgentConfig config) {
        List<NetworkInterface> networkInterfaces = getNetworkInterfaces();
        Boolean isIpv4;
        if (!networkInterfaces.isEmpty()) {
            isIpv4 = preferIpv4(config);
            for (NetworkInterface networkInterface : networkInterfaces) {
                if ((networkInterface.getName().startsWith("eth")) || (networkInterface.getName().startsWith("br"))
                            || (networkInterface.getName().startsWith("wl")) || (networkInterface.getName()
                                                                                         .startsWith("en"))) {
                    InetAddress inetAddress = getInetAddress(networkInterface, isIpv4);
                    if (inetAddress != null) {
                        return inetAddress;
                    }
                }
            }
        }
        return null;
    }

    protected static Boolean preferIpv4(AgentConfig config) {
        Object value = config.getValue("process_host.ipv_preference", null);
        if (value != null) {
            if ("6".equals(String.valueOf(value))) {
                return Boolean.FALSE;
            }
            if ("4".equals(String.valueOf(value))) {
                return Boolean.TRUE;
            }
        }
        return null;
    }

    private static InetAddress getInetAddress(NetworkInterface networkInterface, Boolean isIpv4) {
        List interfaceAddresses = networkInterface.getInterfaceAddresses();
        if (interfaceAddresses == null) {
            return null;
        }
        InetAddress candidate = null;
        Iterator it = interfaceAddresses.iterator();
        while (it.hasNext()) {
            InterfaceAddress interfaceAddress = (InterfaceAddress) it.next();
            InetAddress inetAddress = interfaceAddress.getAddress();
            if (inetAddress != null) {
                if (isIpv4 == null) {
                    candidate = inetAddress;
                } else if ((inetAddress instanceof Inet4Address)) {
                    candidate = inetAddress;
                    if (isIpv4.booleanValue()) {
                        break;
                    }
                } else if ((inetAddress instanceof Inet6Address)) {
                    candidate = inetAddress;
                    if (!isIpv4.booleanValue()) {
                        break;
                    }
                }
            }
        }
        return candidate;
    }
}