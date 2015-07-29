package com.newrelic.agent.instrumentation.weaver;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class InstrumentationMetadata
{
  private final String location;
  private final String implementationTitle;
  private final float implementationVersion;
  private final boolean debug;
  private final boolean enabled;
  private static final Pattern VERSION_PATTERN = Pattern.compile("-");

  public InstrumentationMetadata(JarInputStream jarStream, String location) throws Exception
  {
    this.location = location;

    if (jarStream.getManifest() == null) {
      throw new IOException("The instrumentation jar did not contain a manifest");
    }
    Attributes mainAttributes = jarStream.getManifest().getMainAttributes();
    this.implementationTitle = mainAttributes.getValue("Implementation-Title");
    if (this.implementationTitle == null) {
      throw new Exception("The Implementation-Title of an instrumentation package is undefined");
    }

    String implementationVersion = mainAttributes.getValue("Implementation-Version");
    if (implementationVersion == null) {
      throw new Exception("The Implementation-Version of " + this.implementationTitle + " is undefined");
    }
    implementationVersion = VERSION_PATTERN.split(implementationVersion)[0];
    try {
      this.implementationVersion = Float.parseFloat(implementationVersion);
    } catch (NumberFormatException ex) {
      throw new Exception("The Implementation-Version of " + this.implementationTitle + " (" + implementationVersion + ") cannot be parsed as a float");
    }

    this.debug = Boolean.parseBoolean(mainAttributes.getValue("Debug"));
    String enabled = mainAttributes.getValue("Enabled");
    this.enabled = (enabled == null ? true : Boolean.parseBoolean(enabled));
  }

  public String getLocation() {
    return this.location;
  }

  public String getImplementationTitle() {
    return this.implementationTitle;
  }

  public float getImplementationVersion() {
    return this.implementationVersion;
  }

  public boolean isDebug() {
    return this.debug;
  }

  public boolean isEnabled()
  {
    return this.enabled;
  }
}