package com.newrelic.agent.service;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.logging.IAgentLogger;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

public class ServiceTiming
{
  private static final Comparator<ServiceNameAndTime> serviceNameComparator = new Comparator<ServiceNameAndTime>()
  {
    public int compare(ServiceNameAndTime service1, ServiceNameAndTime service2) {
      return service1.serviceName.compareTo(service2.serviceName);
    }
  };

  private static final Map<ServiceNameAndType, Long> serviceTimings = new LinkedHashMap<ServiceNameAndType, Long>();
  private static final Set<ServiceNameAndTime> serviceInitializationTimings = new TreeSet<ServiceNameAndTime>(serviceNameComparator);

  private static final Set<ServiceNameAndTime> serviceStartTimings = new TreeSet<ServiceNameAndTime>(serviceNameComparator);

  private static volatile long endTimeInNanos = 0L;

  public static void addServiceInitialization(String serviceName) {
    if (serviceName == null) {
      return;
    }
    serviceTimings.put(new ServiceNameAndType(serviceName, Type.initialization), System.nanoTime());
  }

  public static void addServiceStart(String serviceName) {
    if (serviceName == null) {
      return;
    }
    serviceTimings.put(new ServiceNameAndType(serviceName, Type.start), Long.valueOf(System.nanoTime()));
  }

  public static void setEndTime() {
    endTimeInNanos = System.nanoTime();
  }

  public static void logServiceTimings(IAgentLogger logger) {
    boolean startupTimingEnabled = ServiceFactory.getConfigService().getDefaultAgentConfig().isStartupTimingEnabled();
    if ((!startupTimingEnabled) || (logger == null) || (endTimeInNanos == 0L)) {
      serviceTimings.clear();
      return;
    }

    ServiceNameAndType previousServiceNameAndType = null;
    Long previousServiceTime = null;
    for (Entry entry : serviceTimings.entrySet())
    {
      if (previousServiceNameAndType == null) {
        previousServiceNameAndType = (ServiceNameAndType)entry.getKey();
        previousServiceTime = (Long)entry.getValue();
      }
      else
      {
        long serviceTime = (Long) entry.getValue() - previousServiceTime;
        if (previousServiceNameAndType.type == Type.initialization) {
          serviceInitializationTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, serviceTime));
        }
        else {
          serviceStartTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, serviceTime));
        }

        previousServiceNameAndType = (ServiceNameAndType)entry.getKey();
        previousServiceTime = (Long)entry.getValue();
      }
    }
    if ((previousServiceNameAndType != null) && (previousServiceTime != null))
    {
      long serviceTime = endTimeInNanos - previousServiceTime.longValue();
      if (previousServiceNameAndType.type == Type.initialization) {
        serviceInitializationTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, Long.valueOf(serviceTime)));
      }
      else {
        serviceStartTimings.add(new ServiceNameAndTime(previousServiceNameAndType.serviceName, Long.valueOf(serviceTime)));
      }
    }

    for (ServiceNameAndTime entry : serviceInitializationTimings) {
      logger.log(Level.FINEST, "Service Initialization Timing: {0}:{1}ns", new Object[] { entry.serviceName, entry.time });
    }
    for (ServiceNameAndTime entry : serviceStartTimings) {
      logger.log(Level.FINEST, "Service Start Timing: {0}:{1}ns", new Object[] { entry.serviceName, entry.time });
    }

    serviceTimings.clear();
  }

  public static Set<ServiceNameAndTime> getServiceInitializationTimings()
  {
    return serviceInitializationTimings;
  }

  public static Set<ServiceNameAndTime> getServiceStartTimings()
  {
    return serviceStartTimings;
  }

  private static enum Type
  {
    initialization, start;
  }

  private static class ServiceNameAndType
  {
    private final String serviceName;
    private final Type type;

    public ServiceNameAndType(String serviceName, Type type)
    {
      this.serviceName = serviceName;
      this.type = type;
    }

    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      if ((o == null) || (getClass() != o.getClass())) {
        return false;
      }
      ServiceNameAndType that = (ServiceNameAndType)o;

      if (!this.serviceName.equals(that.serviceName))
        return false;
      if (this.type != that.type) {
        return false;
      }
      return true;
    }

    public int hashCode()
    {
      int result = this.serviceName.hashCode();
      result = 31 * result + this.type.hashCode();
      return result;
    }
  }

  public static class ServiceNameAndTime
  {
    private final String serviceName;
    private final Long time;

    public ServiceNameAndTime(String serviceName, Long time)
    {
      this.serviceName = serviceName;
      this.time = time;
    }

    public String getServiceName() {
      return this.serviceName;
    }

    public Long getTime() {
      return this.time;
    }

    public boolean equals(Object o)
    {
      if (this == o)
        return true;
      if ((o == null) || (getClass() != o.getClass())) {
        return false;
      }
      ServiceNameAndTime that = (ServiceNameAndTime)o;

      if (!this.serviceName.equals(that.serviceName))
        return false;
      if (!this.time.equals(that.time)) {
        return false;
      }
      return true;
    }

    public int hashCode()
    {
      int result = this.serviceName.hashCode();
      result = 31 * result + this.time.hashCode();
      return result;
    }
  }
}