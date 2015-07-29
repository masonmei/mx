package com.newrelic.agent.command;

import java.io.File;
import java.text.MessageFormat;

public class XmlInstrumentParams
{
  private File filePath;
  private boolean debug = false;

  public void setFile(String[] pFile, String tagName)
  {
    String fileName = verifyOne(pFile, tagName);
    this.filePath = new File(fileName);
    if (!this.filePath.exists()) {
      throw new IllegalArgumentException(MessageFormat.format("The file specified with the tag {0} does not exist.", new Object[] { tagName }));
    }
    if (!this.filePath.isFile()) {
      throw new IllegalArgumentException(MessageFormat.format("The file specified with the tag {0} must be a file and is not.", new Object[] { tagName }));
    }
    if (!this.filePath.canRead())
      throw new IllegalArgumentException(MessageFormat.format("The file specified with the tag {0} must be readable and is not.", new Object[] { tagName }));
  }

  public File getFile()
  {
    return this.filePath;
  }

  public void setDebug(String[] pDebug, String tagName)
  {
    String value = verifyOneOrNone(pDebug, tagName);
    if (value != null)
      this.debug = Boolean.parseBoolean(value);
  }

  public boolean isDebug()
  {
    return this.debug;
  }

  private String verifyOne(String[] value, String tagName)
  {
    String toReturn = null;
    if ((value != null) && (value.length == 1)) {
      toReturn = value[0];
      if (toReturn != null)
        toReturn = toReturn.trim();
      else
        throw new IllegalArgumentException(MessageFormat.format("One {0}, and only one {0}, must be set.", new Object[] { tagName }));
    }
    else
    {
      throw new IllegalArgumentException(MessageFormat.format("One {0}, and only one {0}, must be set.", new Object[] { tagName }));
    }
    return toReturn;
  }

  private String verifyOneOrNone(String[] value, String tagName)
  {
    String toReturn = null;
    if (value == null)
      return null;
    if (value.length == 1) {
      toReturn = value[0];
      if (toReturn == null) {
        return null;
      }
      toReturn = toReturn.trim();
    }
    else {
      throw new IllegalArgumentException(MessageFormat.format("One {0}, and only one {0}, must be set.", new Object[] { tagName }));
    }
    return toReturn;
  }
}