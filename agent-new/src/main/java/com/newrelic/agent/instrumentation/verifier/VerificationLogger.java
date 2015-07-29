package com.newrelic.agent.instrumentation.verifier;

import com.newrelic.agent.logging.IAgentLogger;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class VerificationLogger
  implements IAgentLogger
{
  private List<String> loggingOutput = new LinkedList();

  private String stackTraceToString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  public List<String> getOutput() {
    return this.loggingOutput;
  }

  public void flush() {
    this.loggingOutput = new LinkedList();
  }

  public boolean isLoggable(Level level)
  {
    return true;
  }

  public void log(Level level, String pattern, Object[] parts)
  {
    this.loggingOutput.add(MessageFormat.format(pattern, parts));
  }

  public void log(Level level, Throwable t, String pattern, Object[] msg)
  {
    this.loggingOutput.add(MessageFormat.format(pattern, msg));
    this.loggingOutput.add(stackTraceToString(t));
  }

  public void logToChild(String childName, Level level, String pattern, Object[] parts)
  {
  }

  public void severe(String message)
  {
    this.loggingOutput.add(message);
  }

  public void error(String message)
  {
    this.loggingOutput.add(message);
  }

  public void warning(String message)
  {
    this.loggingOutput.add(message);
  }

  public void info(String message)
  {
    this.loggingOutput.add(message);
  }

  public void config(String message)
  {
    this.loggingOutput.add(message);
  }

  public void fine(String message)
  {
    this.loggingOutput.add(message);
  }

  public void finer(String message)
  {
    this.loggingOutput.add(message);
  }

  public void finest(String message)
  {
    this.loggingOutput.add(message);
  }

  public void debug(String message)
  {
    this.loggingOutput.add(message);
  }

  public void trace(String message)
  {
    this.loggingOutput.add(message);
  }

  public boolean isFineEnabled()
  {
    return true;
  }

  public boolean isFinerEnabled()
  {
    return true;
  }

  public boolean isFinestEnabled()
  {
    return true;
  }

  public boolean isDebugEnabled()
  {
    return false;
  }

  public boolean isTraceEnabled()
  {
    return false;
  }

  public void log(Level level, String message, Throwable throwable)
  {
  }

  public void log(Level level, String message)
  {
    this.loggingOutput.add(message);
  }

  public void log(Level level, String message, Object[] args, Throwable throwable)
  {
  }

  public IAgentLogger getChildLogger(Class<?> clazz)
  {
    return null;
  }

  public IAgentLogger getChildLogger(String fullName)
  {
    return null;
  }
}