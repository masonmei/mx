package com.newrelic.agent.samplers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Streams;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;

public class ProcStatCPUSampler extends AbstractCPUSampler
{
  private final File statFile;
  private final long clockTicksPerSecond;

  public ProcStatCPUSampler(File statFile)
    throws Exception
  {
    this.statFile = statFile;

    this.clockTicksPerSecond = getClockTicksPerSecond();
    readCPUStats();
  }

  private long getClockTicksPerSecond() {
    long defaultClockTicks = 100L;

    AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
    return ((Long)config.getProperty("clock_ticks_per_second", Long.valueOf(defaultClockTicks))).longValue();
  }

  protected double getProcessCpuTime()
  {
    try {
      CPUStats stats = readCPUStats();
      Agent.LOG.finest("CPU Stats " + stats);
      if (stats == null) {
        return 0.0D;
      }
      return stats.getSystemTime() + stats.getUserTime();
    } catch (IOException e) {
    }
    return 0.0D;
  }

  private CPUStats readCPUStats() throws IOException
  {
    ByteArrayOutputStream oStream = new ByteArrayOutputStream(8192);
    FileInputStream iStream = new FileInputStream(this.statFile);
    String userTime = ""; String systemTime = "";
    try {
      Streams.copy(iStream, oStream);

      oStream.close();
      String[] stats = oStream.toString().split(" ");

      if (stats.length > 13) {
        userTime = stats[13];
        systemTime = stats[14];
        return new CPUStats(Long.parseLong(userTime), Long.parseLong(systemTime));
      }
    }
    catch (NumberFormatException e)
    {
      CPUStats localCPUStats;
      Agent.LOG.fine(MessageFormat.format("Badly formatted CPU jiffies: ''{0}'' user, ''{1}'' system", new Object[] { userTime, systemTime }));

      return null;
    } finally {
      try {
        iStream.close();
      } catch (IOException e) {
      }
    }
    return null;
  }

  private class CPUStats
  {
    private final double userTime;
    private final double systemTime;

    public CPUStats(long userTime, long systemTime) {
      this.userTime = (userTime / ProcStatCPUSampler.this.clockTicksPerSecond);
      this.systemTime = (systemTime / ProcStatCPUSampler.this.clockTicksPerSecond);
    }

    public double getUserTime() {
      return this.userTime;
    }

    public double getSystemTime() {
      return this.systemTime;
    }

    public String toString()
    {
      return "User: " + this.userTime + ", System: " + this.systemTime;
    }
  }
}