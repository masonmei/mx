package com.newrelic.agent.utilization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;

public class MemoryData {
    protected static final Pattern LINUX_MEMORY_PATTERN = Pattern.compile("MemTotal: \\s+(\\d+)\\skB");
    private static Process process = null;

    public static long getTotalRamInMib() {
        String os = ManagementFactory.getOperatingSystemMXBean().getName();

        if (os.contains("Linux")) {
            String match = findMatchInFile(new File("/proc/meminfo"), LINUX_MEMORY_PATTERN);
            if (match != null) {
                long ramInkB = parseLongRam(match);

                return ramInkB / 1024L;
            }
        } else {
            if (os.contains("BSD")) {
                String output = executeCommand("sysctl -n hw.realmem");
                long ramInBytes = parseLongRam(output);
                return ramInBytes / 1048576L;
            }
            if (os.contains("Mac")) {
                String output = executeCommand("sysctl -n hw.memsize");
                long ramInBytes = parseLongRam(output);
                return ramInBytes / 1048576L;
            }
            Agent.LOG.log(Level.FINER, MessageFormat.format("Could not get total physical memory for OS {0}",
                                                                   new Object[] {os}));
        }
        return 0L;
    }

    protected static String findMatchInFile(File file, Pattern lookFor) {
        if ((file.exists()) && (file.canRead())) {
            BufferedReader reader = null;
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                reader = new BufferedReader(inputStreamReader);

                Matcher matcher = lookFor.matcher("");
                String line;
                while ((line = reader.readLine()) != null) {
                    matcher.reset(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                }
            }
        } else {
            Agent.LOG.log(Level.FINER, MessageFormat.format("Could not read file {0}", new Object[] {file.getName()}));
        }
        return null;
    }

    protected static long parseLongRam(String number) {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            Agent.LOG.log(Level.FINE, MessageFormat.format("Unable to parse total memory available. Found {0}",
                                                                  new Object[] {number}));
        }
        return 0L;
    }

    protected static String executeCommand(String command) {
        StringBuffer output = new StringBuffer();
        try {
            process = Runtime.getRuntime().exec(command);
            BufferedReader procOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = procOutput.readLine()) != null) {
                output.append(line);
            }
            process.waitFor();
        } catch (IOException e) {
            Agent.LOG.log(Level.FINEST, MessageFormat.format("An exception ocurred {0}", new Object[] {e}));
        } catch (InterruptedException e) {
            Agent.LOG.log(Level.FINER, "Memory utilization task was interrupted.");
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

        return output.toString();
    }

    public static void cleanup() {
        if (process != null) {
            process.destroy();
        }
    }
}