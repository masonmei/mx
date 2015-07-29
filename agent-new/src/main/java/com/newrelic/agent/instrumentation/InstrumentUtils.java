package com.newrelic.agent.instrumentation;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class InstrumentUtils
{
  public static String getURI(URI theUri)
  {
    if (theUri == null) {
      return "";
    }
    return getURI(theUri.getScheme(), theUri.getHost(), theUri.getPort(), theUri.getPath());
  }

  public static String getURI(URL theUrl)
  {
    if (theUrl == null)
      return "";
    try
    {
      return getURI(theUrl.toURI()); } catch (URISyntaxException e) {
    }
    return getURI(theUrl.getProtocol(), theUrl.getHost(), theUrl.getPort(), theUrl.getPath());
  }

  public static String getURI(String scheme, String host, int port, String path)
  {
    StringBuilder sb = new StringBuilder();
    if (scheme != null) {
      sb.append(scheme);
      sb.append("://");
    }
    if (host != null) {
      sb.append(host);
      if (port >= 0) {
        sb.append(":");
        sb.append(port);
      }
    }
    if (path != null) {
      sb.append(path);
    }
    return sb.toString();
  }

  public static void setFinal(Object context, Field field, Object newValue)
    throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
  {
    field.setAccessible(true);

    Field modifiersField = Field.class.getDeclaredField("modifiers");
    boolean wasAccessible = modifiersField.isAccessible();

    modifiersField.setAccessible(true);

    field.set(context, newValue);

    modifiersField.setAccessible(wasAccessible);
  }
}