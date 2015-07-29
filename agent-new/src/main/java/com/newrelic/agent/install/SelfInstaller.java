package com.newrelic.agent.install;

import com.newrelic.agent.util.EditableFile;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class SelfInstaller
{
  public static final String lineSep = System.getProperty("line.separator");
  public static final String fileSep = System.getProperty("file.separator");
  static final String DOTBAT = ".bat";
  static final String DOTSH = ".sh";
  static final String DOTCONF = ".conf";
  public OS os;
  public boolean osIsMac;
  public boolean osIsUnix;
  public boolean osIsWindows;
  private DateFormat df;

  public SelfInstaller()
  {
    this.df = new SimpleDateFormat("yyyy MMM dd, HH:mm:ss");
    this.os = getOS();
    if (this.os == OS.MAC) {
      this.osIsMac = true;
      this.osIsUnix = false;
      this.osIsWindows = false;
    } else if (this.os == OS.UNIX) {
      this.osIsMac = false;
      this.osIsUnix = true;
      this.osIsWindows = false;
    } else if (this.os == OS.WINDOWS) {
      this.osIsMac = false;
      this.osIsUnix = false;
      this.osIsWindows = true;
    }
  }

  public abstract boolean backupAndEditStartScript(String paramString);

  public abstract String getStartScript();

  public abstract String getAlternateStartScript();

  public abstract String getLocator();

  public abstract String getAlternateLocator();

  public abstract String getAgentSettings();

  public abstract String getAgentAlreadySetExpr();

  public OS getOS()
  {
    String osName = System.getProperty("os.name");
    if (osName.toLowerCase().startsWith("windows"))
      return OS.WINDOWS;
    if (osName.toLowerCase().startsWith("mac")) {
      return OS.MAC;
    }
    return OS.UNIX;
  }

  public String getCommentForAgentSwitch(String commentChars)
  {
    return commentChars + " ---- New Relic switch automatically added to start command on " + this.df.format(new Date());
  }

  public void backup(EditableFile file)
  {
    String backedUpFile = file.backup();
    if (!backedUpFile.equals(""))
      System.out.println("Backed up start script to " + backedUpFile);
  }

  public static enum OS
  {
    MAC, UNIX, WINDOWS;
  }
}