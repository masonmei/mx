package com.newrelic.agent.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.newrelic.agent.service.ServiceFactory;

public class StackTraces {
    public static StackTraceElement[] getThreadStackTraceElements(long threadId) {
        ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(threadId, 2147483647);
        if (threadInfo == null) {
            return null;
        }
        return threadInfo.getStackTrace();
    }

    public static Exception createStackTraceException(String message) {
        StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();
        return createStackTraceException(message, stackTraces, true);
    }

    public static Exception createStackTraceException(String message, StackTraceElement[] stackTraces, boolean scrub) {
        return createStackTraceException(new Exception(message), stackTraces, scrub);
    }

    public static Exception createStackTraceException(Exception e, StackTraceElement[] stackTraces, boolean scrub) {
        List scrubbedTrace = scrub ? scrubAndTruncate(stackTraces) : Arrays.asList(stackTraces);

        e.setStackTrace((StackTraceElement[]) scrubbedTrace.toArray(new StackTraceElement[0]));
        return e;
    }

    public static List<String> toStringList(List<StackTraceElement> stackElements) {
        List stringList = new ArrayList(stackElements.size());
        for (StackTraceElement element : stackElements) {
            stringList.add(element.toString());
        }
        return stringList;
    }

    public static List<String> toStringListRemoveParent(List<StackTraceElement> stackElements,
                                                        List<StackTraceElement> parentBacktrace) {
        if ((parentBacktrace == null) || (parentBacktrace.size() <= 1)) {
            return toStringList(stackElements);
        }

        parentBacktrace = scrubAndTruncate(parentBacktrace);

        if ((parentBacktrace == null) || (parentBacktrace.size() <= 1)) {
            return toStringList(stackElements);
        }

        StackTraceElement parentLatestFirst = (StackTraceElement) parentBacktrace.get(0);
        StackTraceElement parentLatestSecond = (StackTraceElement) parentBacktrace.get(1);
        List stringList = new ArrayList();
        int currentLength = stackElements.size();

        for (int i = 0; i < currentLength; i++) {
            StackTraceElement current = (StackTraceElement) stackElements.get(i);

            if ((isSameClassAndMethod(current, parentLatestFirst)) && (i + 1 < currentLength)
                        && (((StackTraceElement) stackElements.get(i + 1)).equals(parentLatestSecond))) {
                break;
            }
            stringList.add(current.toString());
        }

        return stringList;
    }

    protected static boolean isSameClassAndMethod(StackTraceElement one, StackTraceElement two) {
        if (one == two) {
            return true;
        }
        return (one.getClassName().equals(two.getClassName())) && (one.getMethodName().equals(two.getMethodName()));
    }

    public static List<StackTraceElement> scrubAndTruncate(StackTraceElement[] stackTraces) {
        return scrubAndTruncate(Arrays.asList(stackTraces));
    }

    public static List<StackTraceElement> scrubAndTruncate(List<StackTraceElement> stackTraces) {
        return scrubAndTruncate(stackTraces, ServiceFactory.getConfigService().getDefaultAgentConfig()
                                                     .getMaxStackTraceLines());
    }

    public static List<StackTraceElement> scrubAndTruncate(List<StackTraceElement> stackTraces,
                                                           int maxStackTraceLines) {
        List trimmedList = scrub(stackTraces);
        return maxStackTraceLines > 0 ? truncateStack(trimmedList, maxStackTraceLines) : trimmedList;
    }

    public static List<StackTraceElement> scrub(List<StackTraceElement> stackTraces) {
        for (int i = stackTraces.size() - 1; i >= 0; i--) {
            StackTraceElement element = (StackTraceElement) stackTraces.get(i);

            if ((element.getClassName().startsWith("com.newrelic.agent.")) || (element.getClassName()
                                                                                       .startsWith("com.newrelic.bootstrap."))
                        || (element.getClassName().startsWith("com.newrelic.api.agent.")) || (("getAgentHandle"
                                                                                                       .equals(element.getMethodName()))
                                                                                                      && (("java.lanreflect.Proxy")
                                                                                                                  .equals(element.getClassName())))) {
                return stackTraces.subList(i + 1, stackTraces.size());
            }
        }
        return stackTraces;
    }

    public static List<StackTraceElement> last(StackTraceElement[] elements, int count) {
        List list = Arrays.asList(elements);
        if (list.size() <= count) {
            return list;
        }
        return list.subList(list.size() - count, list.size());
    }

    static List<StackTraceElement> truncateStack(List<StackTraceElement> elements, int maxDepth) {
        if (elements.size() <= maxDepth) {
            return elements;
        }

        int bottomLimit = Double.valueOf(Math.floor(maxDepth / 3)).intValue();
        int topLimit = maxDepth - bottomLimit;

        List topStack = elements.subList(0, topLimit);
        List bottomStack = elements.subList(elements.size() - bottomLimit, elements.size());
        int skipCount = elements.size() - bottomLimit - topLimit;

        elements = new ArrayList(maxDepth + 1);
        elements.addAll(topStack);
        elements.add(new StackTraceElement("Skipping " + skipCount + " lines...", "", "", 0));
        elements.addAll(bottomStack);

        return elements;
    }

    public static Throwable getRootCause(Throwable throwable) {
        return throwable.getCause() == null ? throwable : throwable.getCause();
    }

    public static Collection<String> stackTracesToStrings(StackTraceElement[] stackTraces) {
        if ((stackTraces == null) || (stackTraces.length == 0)) {
            return Collections.emptyList();
        }
        List lines = new ArrayList(stackTraces.length);
        for (StackTraceElement e : stackTraces) {
            lines.add('\t' + e.toString());
        }

        return lines;
    }

    public static boolean isInAgentInstrumentation(StackTraceElement[] stackTrace) {
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().startsWith("com.newrelic.agent.")) {
                return true;
            }
        }
        return false;
    }
}