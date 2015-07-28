/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package com.newrelic.deps.ch.qos.logback.core.util;

/**
 * @author Ceki G&uuml;c&uuml;
 */
public class EnvUtil {


  static public boolean isJDK5() {
    String javaVersion = System.getProperty("java.version");
    if (javaVersion == null) {
      return false;
    }
    if (javaVersion.startsWith("1.5")) {
      return true;
    } else {
      return false;
    }
  }

  static public boolean isJaninoAvailable() {
    ClassLoader classLoader = EnvUtil.class.getClassLoader();
    try {
      Class bindingClass = classLoader.loadClass("org.codehaus.janino.ScriptEvaluator");
      return (bindingClass != null);
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static boolean isWindows() {
    String os = System.getProperty("os.name");
    return os.startsWith("Windows");
  }

}
