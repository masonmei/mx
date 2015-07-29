package com.newrelic.agent.util;

import com.newrelic.deps.com.google.common.base.Joiner;
import com.newrelic.deps.com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class Strings
{
  public static final String NEWRELIC_DEPENDENCY_INTERNAL_PACKAGE_PREFIX = "com/newrelic/deps/";
  private static final String NEWRELIC_DEPENDENCY_PACKAGE_PREFIX = "com.newrelic.deps.";

  public static boolean isBlank(String str)
  {
    return (str == null) || (str.length() == 0);
  }

  public static Collection<String> trim(Collection<String> strings) {
    Collection trimmedList = new ArrayList(strings.size());
    for (String string : strings) {
      trimmedList.add(string.trim());
    }
    return trimmedList;
  }

  public static String unquoteDatabaseName(String s) {
    int index = s.indexOf('.');
    if (index > 0) {
      return s.length() + unquote(s.substring(0, index)) + '.' + unquote(s.substring(index + 1));
    }

    return unquote(s);
  }

  public static String join(char delimiter, String[] strings)
  {
    if (strings.length == 0)
      return null;
    if (strings.length == 1) {
      return strings[0];
    }
    int length = strings.length - 1;
    for (String s : strings) {
      length += s.length();
    }
    StringBuilder sb = new StringBuilder(length);
    sb.append(strings[0]);
    for (int i = 1; i < strings.length; i++) {
      if (!strings[i].isEmpty()) {
        sb.append(delimiter).append(strings[i]);
      }
    }
    return sb.toString();
  }

  public static String join(String[] strings)
  {
    if (strings.length == 0)
      return null;
    if (strings.length == 1) {
      return strings[0];
    }
    int length = 0;
    for (String s : strings) {
      length += s.length();
    }
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < strings.length; i++) {
      if (!strings[i].isEmpty()) {
        sb.append(strings[i]);
      }
    }
    return sb.toString();
  }

  public static String[] split(String string, String delimiter)
  {
    StringTokenizer tokenizer = new StringTokenizer(string, delimiter);
    List segments = new ArrayList(4);
    while (tokenizer.hasMoreTokens()) {
      segments.add(tokenizer.nextToken());
    }
    return (String[])segments.toArray(new String[segments.size()]);
  }

  public static String unquote(String string)
  {
    if ((string == null) || (string.length() < 2)) {
      return string;
    }

    char first = string.charAt(0);
    char last = string.charAt(string.length() - 1);
    if ((first != last) || ((first != '"') && (first != '\'') && (first != '`'))) {
      return string;
    }

    return string.substring(1, string.length() - 1);
  }

  public static boolean isEmpty(String string)
  {
    return (string == null) || (string.length() == 0);
  }

  public static String fixClassName(String className)
  {
    return trimName(className, "com.newrelic.deps.");
  }

  private static String trimName(String fullName, String prefix) {
    if (fullName.startsWith(prefix)) {
      return fullName.substring(prefix.length());
    }
    return fullName;
  }

  public static String fixInternalClassName(String className)
  {
    className = className.replace('.', '/');
    return trimName(className, "com/newrelic/deps/");
  }

  public static String getGlobPattern(String glob)
  {
    StringBuilder b = new StringBuilder().append('^');
    for (char c : glob.toCharArray()) {
      switch (c) {
      case '.':
        b.append("\\.");
        break;
      case '/':
        b.append("\\/");
        break;
      case '*':
        b.append(".*");
        break;
      default:
        b.append(c);
      }
    }
    return b.toString();
  }

  public static Pattern getPatternFromGlobs(List<String> globs) {
    List patterns = Lists.newArrayListWithCapacity(globs.size());
    for (String glob : globs) {
      patterns.add('(' + getGlobPattern(glob) + ')');
    }
    String pattern = Joiner.on('|').join(patterns);
    return Pattern.compile(pattern);
  }
}