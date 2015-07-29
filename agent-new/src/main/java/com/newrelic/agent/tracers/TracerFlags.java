package com.newrelic.agent.tracers;

public final class TracerFlags
{
  public static final int GENERATE_SCOPED_METRIC = 2;
  public static final int TRANSACTION_TRACER_SEGMENT = 4;
  public static final int DISPATCHER = 8;
  public static final int CUSTOM = 16;
  public static final int LEAF = 32;

  public static boolean isDispatcher(int flags)
  {
    return (flags & 0x8) == 8;
  }

  public static boolean isCustom(int flags) {
    return (flags & 0x10) == 16;
  }

  public static int clearSegment(int flags) {
    return flags & 0xFFFFFFFB;
  }
}