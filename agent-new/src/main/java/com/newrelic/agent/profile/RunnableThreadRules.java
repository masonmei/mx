package com.newrelic.agent.profile;

import java.lang.management.ThreadInfo;

public class RunnableThreadRules
{
  public boolean isRunnable(ThreadInfo threadInfo)
  {
    if (!Thread.State.RUNNABLE.equals(threadInfo.getThreadState())) {
      return false;
    }
    return isRunnable(threadInfo.getStackTrace());
  }

  public boolean isRunnable(StackTraceElement[] elements) {
    if (elements.length == 0) {
      return false;
    }
    return isRunnable(elements[0]);
  }

  public boolean isRunnable(StackTraceElement firstElement) {
    String className = firstElement.getClassName();
    String methodName = firstElement.getMethodName();
    if ((Object.class.getName().equals(className)) && ("wait".equals(methodName))) {
      return false;
    }
    if (!firstElement.isNativeMethod()) {
      return true;
    }
    if (className.startsWith("java.io.")) {
      return false;
    }
    if (className.startsWith("java.net.")) {
      return false;
    }
    if (className.startsWith("sun.nio.")) {
      return false;
    }
    if ("jrockit.net.SocketNativeIO".equals(className)) {
      return false;
    }
    if (("java.lang.UNIXProcess".equals(className)) && ("waitForProcessExit".equals(methodName))) {
      return false;
    }
    if (("sun.misc.Unsafe".equals(className)) && ("park".equals(methodName))) {
      return false;
    }
    if (("org.apache.tomcat.jni.Socket".equals(className)) && ("accept".equals(methodName))) {
      return false;
    }
    if (("org.apache.tomcat.jni.Poll".equals(className)) && ("poll".equals(methodName))) {
      return false;
    }
    if (("weblogic.socket.PosixSocketMuxer".equals(className)) && ("poll".equals(methodName))) {
      return false;
    }
    if (("weblogic.socket.NTSocketMuxer".equals(className)) && ("getIoCompletionResult".equals(methodName))) {
      return false;
    }
    if (("com.caucho.vfs.JniServerSocketImpl".equals(className)) && ("nativeAccept".equals(methodName))) {
      return false;
    }
    return true;
  }
}