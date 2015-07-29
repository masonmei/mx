//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.profile;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.deps.org.json.simple.JSONArray;

import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.profile.ThreadType.BasicThreadType;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transport.DataSenderWriter;
import com.newrelic.agent.util.StackTraces;

public class Profile implements IProfile {
  public static final int MAX_STACK_DEPTH = 300;
  public static final int MAX_STACK_SIZE = 60000;
  public static final int MAX_ENCODED_BYTES = 1000000;
  public static final int STACK_TRIM = 10000;
  private final ProfilerParameters profilerParameters;
  private final Map<ThreadType, ProfileTree> profileTrees = new HashMap();
  private long startTimeMillis = 0L;
  private long endTimeMillis = 0L;
  private int sampleCount = 0;
  private int totalThreadCount = 0;
  private int runnableThreadCount = 0;
  private Map<Long, Long> startThreadCpuTimes;

  public Profile(ProfilerParameters parameters) {
    this.profilerParameters = parameters;
  }

  private Map<Long, Long> getThreadCpuTimes() {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    if (threadMXBean.isThreadCpuTimeSupported() && threadMXBean.isThreadCpuTimeEnabled()) {
      HashMap cpuTimes = new HashMap();
      long[] arr$ = threadMXBean.getAllThreadIds();
      int len$ = arr$.length;

      for (int i$ = 0; i$ < len$; ++i$) {
        long id = arr$[i$];
        cpuTimes.put(Long.valueOf(id), Long.valueOf(threadMXBean.getThreadCpuTime(id)));
      }

      return cpuTimes;
    } else {
      return Collections.emptyMap();
    }
  }

  public ProfileTree getProfileTree(ThreadType threadType) {
    ProfileTree profileTree = (ProfileTree) this.profileTrees.get(threadType);
    if (profileTree == null) {
      profileTree = new ProfileTree();
      this.profileTrees.put(threadType, profileTree);
    }

    return profileTree;
  }

  public void start() {
    this.startTimeMillis = System.currentTimeMillis();
    this.startThreadCpuTimes = this.getThreadCpuTimes();
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    if (!threadMXBean.isThreadCpuTimeSupported()) {
      Agent.LOG.info("Profile unable to record CPU time: Thread CPU time measurement is not supported");
    } else if (!threadMXBean.isThreadCpuTimeEnabled()) {
      Agent.LOG.info("Profile unable to record CPU time: Thread CPU time measurement is not enabled");
    }

  }

  public void end() {
    this.endTimeMillis = System.currentTimeMillis();
    Map endThreadCpuTimes = this.getThreadCpuTimes();
    ThreadService threadService = ServiceFactory.getThreadService();
    Set requestThreadIds = threadService.getRequestThreadIds();
    Set backgroundThreadIds = threadService.getBackgroundThreadIds();
    Set agentThreadIds = threadService.getAgentThreadIds();

    long cpuTime;
    ProfileTree tree;
    for (Iterator stackCount = endThreadCpuTimes.entrySet().iterator(); stackCount.hasNext();
         tree.incrementCpuTime(cpuTime)) {
      Entry msg = (Entry) stackCount.next();
      Long startTime = (Long) this.startThreadCpuTimes.get(msg.getKey());
      if (startTime == null) {
        startTime = Long.valueOf(0L);
      }

      cpuTime = TimeUnit.MILLISECONDS.convert(((Long) msg.getValue()).longValue() - startTime.longValue(),
                                                     TimeUnit.NANOSECONDS);
      if (requestThreadIds.contains(msg.getKey())) {
        tree = this.getProfileTree(BasicThreadType.REQUEST);
      } else if (backgroundThreadIds.contains(msg.getKey())) {
        tree = this.getProfileTree(BasicThreadType.BACKGROUND);
      } else if (agentThreadIds.contains(msg.getKey())) {
        tree = this.getProfileTree(BasicThreadType.AGENT);
      } else {
        tree = this.getProfileTree(BasicThreadType.OTHER);
      }
    }

    int stackCount1 = this.getCallSiteCount();
    String msg1 =
            MessageFormat.format("Profile size is {0} stack elements", new Object[] {Integer.valueOf(stackCount1)});
    Agent.LOG.info(msg1);
    if (stackCount1 > '\uea60') {
      Agent.LOG.info(MessageFormat.format("Trimmed profile size by {0} stack elements",
                                                 new Object[] {Integer.valueOf(this.trim(stackCount1 - '\uea60',
                                                                                                stackCount1))}));
    }

  }

  public void markInstrumentedMethods() {
    try {
      this.doMarkInstrumentedMethods();
    } catch (Throwable var3) {
      String msg = MessageFormat.format("Error marking instrumented methods {0}", new Object[] {var3});
      if (Agent.LOG.isLoggable(Level.FINEST)) {
        Agent.LOG.log(Level.FINEST, msg, var3);
      } else {
        Agent.LOG.finer(msg);
      }
    }

  }

  private void doMarkInstrumentedMethods() {
    Class[] allLoadedClasses = ServiceFactory.getAgent().getInstrumentation().getAllLoadedClasses();
    HashMap classMap = Maps.newHashMap();
    Class[] i$ = allLoadedClasses;
    int tree = allLoadedClasses.length;

    for (int i$1 = 0; i$1 < tree; ++i$1) {
      Class clazz = i$[i$1];
      classMap.put(clazz.getName(), clazz);
    }

    Iterator var7 = this.profileTrees.values().iterator();

    while (var7.hasNext()) {
      ProfileTree var8 = (ProfileTree) var7.next();
      var8.setMethodDetails(classMap);
    }

  }

  public int trimBy(int limit) {
    return this.trim(limit, this.getCallSiteCount());
  }

  private int trim(int limit, int stackCount) {
    Profile.ProfileSegmentSort[] segments = this.getSortedSegments(stackCount);
    int count = 0;
    Profile.ProfileSegmentSort[] arr$ = segments;
    int len$ = segments.length;

    for (int i$ = 0; i$ < len$; ++i$) {
      Profile.ProfileSegmentSort segment = arr$[i$];
      if (count >= limit) {
        break;
      }

      segment.remove();
      ++count;
    }

    return count;
  }

  private Profile.ProfileSegmentSort[] getSortedSegments(int stackCount) {
    Profile.ProfileSegmentSort[] segments = new Profile.ProfileSegmentSort[stackCount];
    int index = 0;
    Iterator i$ = this.profileTrees.values().iterator();

    while (i$.hasNext()) {
      ProfileTree profileTree = (ProfileTree) i$.next();

      ProfileSegment rootSegment;
      for (Iterator i$1 = profileTree.getRootSegments().iterator(); i$1.hasNext();
           index = this.addSegment(rootSegment, (ProfileSegment) null, 1, segments, index)) {
        rootSegment = (ProfileSegment) i$1.next();
      }
    }

    Arrays.sort(segments);
    return segments;
  }

  private int addSegment(ProfileSegment segment, ProfileSegment parent, int depth,
                         Profile.ProfileSegmentSort[] segments, int index) {
    Profile.ProfileSegmentSort segSort = new Profile.ProfileSegmentSort(segment, parent, depth);
    segments[index++] = segSort;

    ProfileSegment child;
    for (Iterator i$ = segment.getChildren().iterator(); i$.hasNext();
         index = this.addSegment(child, segment, depth, segments, index)) {
      child = (ProfileSegment) i$.next();
      ++depth;
    }

    return index;
  }

  private int getCallSiteCount() {
    int count = 0;

    ProfileTree profileTree;
    for (Iterator i$ = this.profileTrees.values().iterator(); i$.hasNext();
         count += profileTree.getCallSiteCount()) {
      profileTree = (ProfileTree) i$.next();
    }

    return count;
  }

  public Long getProfileId() {
    return this.profilerParameters.getProfileId();
  }

  public ProfilerParameters getProfilerParameters() {
    return this.profilerParameters;
  }

  public void beforeSampling() {
    ++this.sampleCount;
  }

  public int getSampleCount() {
    return this.sampleCount;
  }

  public final long getStartTimeMillis() {
    return this.startTimeMillis;
  }

  public final long getEndTimeMillis() {
    return this.endTimeMillis;
  }

  public void writeJSONString(Writer out) throws IOException {
    if (this.profilerParameters.getXraySessionId() == null) {
      JSONArray.writeJSONString(Arrays.asList(new Serializable[] {this.profilerParameters.getProfileId(),
                                                                         Long.valueOf(this.startTimeMillis),
                                                                         Long.valueOf(this.endTimeMillis),
                                                                         Integer.valueOf(this.sampleCount),
                                                                         this.compressedData(out),
                                                                         Integer.valueOf(this.totalThreadCount),
                                                                         Integer.valueOf(this.runnableThreadCount)}),
                                       out);
    } else {
      JSONArray.writeJSONString(Arrays.asList(new Serializable[] {this.profilerParameters.getProfileId(),
                                                                         Long.valueOf(this.startTimeMillis),
                                                                         Long.valueOf(this.endTimeMillis),
                                                                         Integer.valueOf(this.sampleCount),
                                                                         this.compressedData(out),
                                                                         Integer.valueOf(this.totalThreadCount),
                                                                         Integer.valueOf(this.runnableThreadCount),
                                                                         this.profilerParameters
                                                                                 .getXraySessionId()}), out);
    }

  }

  private String compressedData(Writer out) {
    String result = DataSenderWriter.getJsonifiedCompressedEncodedString(this.profileTrees, out, 1);

    for (int maxStack = '\uea60'; result.length() > 1000000 && maxStack > 0;
         result = DataSenderWriter.getJsonifiedCompressedEncodedString(this.profileTrees, out, 1)) {
      maxStack -= 10000;
      int msg = this.getCallSiteCount();
      this.trim(msg - maxStack, msg);
    }

    if (DataSenderWriter.isCompressingWriter(out)) {
      String msg1 = MessageFormat.format("Profile serialized size = {0} bytes",
                                                new Object[] {Integer.valueOf(result.length())});
      Agent.LOG.info(msg1);
    }

    return result;
  }

  private void incrementThreadCounts(boolean runnable) {
    ++this.totalThreadCount;
    if (runnable) {
      ++this.runnableThreadCount;
    }

  }

  private boolean shouldScrubStack(ThreadType type) {
    return BasicThreadType.AGENT.equals(type) ? false : !this.profilerParameters.isProfileAgentThreads();
  }

  public void addStackTrace(long threadId, boolean runnable, ThreadType type, StackTraceElement... stackTrace) {
    if (stackTrace.length >= 2) {
      this.incrementThreadCounts(runnable);
      List stackTraceList;
      if (this.shouldScrubStack(type)) {
        stackTraceList = StackTraces.scrubAndTruncate(Arrays.asList(stackTrace), 0);
      } else {
        stackTraceList = Arrays.asList(stackTrace);
      }

      ArrayList result = new ArrayList(stackTraceList);
      Collections.reverse(result);
      this.getProfileTree(type).addStackTrace(result, runnable);
    }
  }

  private static class ProfileSegmentSort implements Comparable<Profile.ProfileSegmentSort> {
    private final ProfileSegment segment;
    private final ProfileSegment parent;
    private final int depth;

    private ProfileSegmentSort(ProfileSegment segment, ProfileSegment parent, int depth) {
      this.segment = segment;
      this.parent = parent;
      this.depth = depth;
    }

    void remove() {
      if (this.parent != null) {
        this.parent.removeChild(this.segment.getMethod());
      }

    }

    public String toString() {
      return this.segment.toString();
    }

    public int compareTo(Profile.ProfileSegmentSort other) {
      int thisCount = this.segment.getRunnableCallCount();
      int otherCount = other.segment.getRunnableCallCount();
      return thisCount == otherCount ? (this.depth > other.depth ? -1 : (this.depth == other.depth ? 0 : 1))
                     : (thisCount > otherCount ? 1 : -1);
    }
  }
}
