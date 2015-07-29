package com.newrelic.api.agent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.attributes.CustomAttributeSender;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.browser.BrowserTransactionState;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.dispatchers.WebRequestDispatcher;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import com.newrelic.agent.util.Strings;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

public class NewRelicApiImplementation
  implements PublicApi
{
  private final AttributeSender attributeSender = new CustomAttributeSender();

  public void noticeError(Throwable throwable, Map<String, String> params)
  {
    try
    {
      ErrorService.reportException(throwable, filtorErrorAtts(params, this.attributeSender));
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Reported error: {0}", new Object[] { throwable });
        Agent.LOG.finer(msg);
      }
    } catch (Throwable t) {
      String msg = MessageFormat.format("Exception reporting exception \"{0}\": {1}", new Object[] { throwable, t });
      logException(msg, t);
    }
  }

  public void noticeError(Throwable throwable)
  {
    Map params = Collections.emptyMap();
    noticeError(throwable, params);
  }

  public void noticeError(String message, Map<String, String> params)
  {
    try
    {
      ErrorService.reportError(message, filtorErrorAtts(params, this.attributeSender));
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Reported error: {0}", new Object[] { message });
        Agent.LOG.finer(msg);
      }
    } catch (Throwable t) {
      String msg = MessageFormat.format("Exception reporting exception \"{0}\": {1}", new Object[] { message, t });
      logException(msg, t);
    }
  }

  private static Map<String, String> filtorErrorAtts(Map<String, String> params, AttributeSender attributeSender) {
    Map atts = new TreeMap();
    int maxErrorCount;
    if (params != null) {
      maxErrorCount = getNumberOfErrorAttsLeft();

      for (Entry current : params.entrySet())
      {
        if (atts.size() >= maxErrorCount)
        {
          Agent.LOG.log(Level.FINER, "Unable to add custom attribute for key \"{0}\" because the limit on error attributes has been reached.", new Object[] { current.getKey() });
        }
        else
        {
          Object value = attributeSender.verifyParameterAndReturnValue((String)current.getKey(), current.getValue(), "noticeError");

          if (value != null) {
            atts.put(current.getKey(), (String)value);
          }
        }
      }
    }
    return atts;
  }

  private static int getNumberOfErrorAttsLeft() {
    Transaction tx = Transaction.getTransaction();
    return tx.getAgentConfig().getMaxUserParameters() - tx.getErrorAttributes().size();
  }

  public void noticeError(String message)
  {
    Map params = Collections.emptyMap();
    noticeError(message, params);
  }

  public void addCustomParameter(String key, String value)
  {
    this.attributeSender.addAttribute(key, value, "addCustomParameter");
  }

  public void addCustomParameter(String key, Number value)
  {
    this.attributeSender.addAttribute(key, value, "addCustomParameter");
  }

  public void setTransactionName(String category, String name)
  {
    if (Strings.isEmpty(category)) {
      category = "Custom";
    }
    if ((name == null) || (name.length() == 0)) {
      Agent.LOG.log(Level.FINER, "Unable to set the transaction name to an empty string");
      return;
    }
    if (!name.startsWith("/")) {
      name = "/" + name;
    }

    Transaction tx = Transaction.getTransaction().getRootTransaction();
    Dispatcher dispatcher = tx.getDispatcher();
    if (dispatcher == null) {
      if (Agent.LOG.isFinerEnabled()) {
        Agent.LOG.finer(MessageFormat.format("Unable to set the transaction name to \"{0}\" in NewRelic API - no transaction", new Object[] { name }));
      }

      return;
    }

    boolean isWebTransaction = dispatcher.isWebTransaction();

    TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
    TransactionNamePriority namePriority = "Uri".equals(category) ? TransactionNamePriority.REQUEST_URI : TransactionNamePriority.CUSTOM_HIGH;

    if (Agent.LOG.isLoggable(Level.FINER)) {
      if (policy.canSetTransactionName(tx, namePriority)) {
        String msg = MessageFormat.format("Setting {1} transaction name to \"{0}\" in NewRelic API", new Object[] { name, isWebTransaction ? "web" : "background" });

        Agent.LOG.finer(msg);
      } else {
        Agent.LOG.finer("Unable to set the transaction name to " + name);
      }
    }
    synchronized (tx) {
      policy.setTransactionName(tx, name, category, namePriority);
    }
  }

  public void ignoreTransaction()
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    synchronized (tx) {
      tx.setIgnore(true);
    }
    if (Agent.LOG.isLoggable(Level.FINER))
      Agent.LOG.finer("Set ignore transaction in NewRelic API");
  }

  public void ignoreApdex()
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    synchronized (tx) {
      tx.ignoreApdex();
    }
    if (Agent.LOG.isLoggable(Level.FINER))
      Agent.LOG.finer("Set ignore APDEX in NewRelic API");
  }

  public void setRequestAndResponse(Request request, Response response)
  {
    Transaction tx = Transaction.getTransaction();
    Dispatcher dispatcher = new WebRequestDispatcher(request, response, tx);
    tx.setDispatcher(dispatcher);
    Agent.LOG.finest("Custom request dispatcher registered");
  }

  public static String getBrowserTimingHeaderForContentType(String contentType)
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    try {
      if (!tx.isStarted()) {
        Agent.LOG.finer("Unable to inject browser timing header in a JSP: not running in a transaction");
        return "";
      }
      String header = null;
      synchronized (tx) {
        header = tx.getBrowserTransactionState().getBrowserTimingHeaderForJsp();
      }
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Injecting browser timing header in a JSP: {0}", new Object[] { header });
        Agent.LOG.log(Level.FINER, msg);
      }
      return header;
    } catch (Throwable t) {
      String msg = MessageFormat.format("Error injecting browser timing header in a JSP: {0}", new Object[] { t });
      logException(msg, t);
    }return "";
  }

  public String getBrowserTimingHeader()
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    try {
      if (!tx.isStarted()) {
        Agent.LOG.finer("Unable to get browser timing header in NewRelic API: not running in a transaction");
        return "";
      }
      String header = null;
      synchronized (tx) {
        header = tx.getBrowserTransactionState().getBrowserTimingHeader();
      }
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Got browser timing header in NewRelic API: {0}", new Object[] { header });
        Agent.LOG.log(Level.FINER, msg);
      }
      return header;
    } catch (Throwable t) {
      String msg = MessageFormat.format("Error getting browser timing header in NewRelic API: {0}", new Object[] { t });
      logException(msg, t);
    }return "";
  }

  public static String getBrowserTimingFooterForContentType(String contentType)
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    try {
      if (!tx.isStarted()) {
        Agent.LOG.finer("Unable to inject browser timing footer in a JSP: not running in a transaction");
        return "";
      }
      String footer = null;
      synchronized (tx) {
        footer = tx.getBrowserTransactionState().getBrowserTimingFooter();
      }
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Injecting browser timing footer in a JSP: {0}", new Object[] { footer });
        Agent.LOG.log(Level.FINER, msg);
      }
      return footer;
    } catch (Throwable t) {
      String msg = MessageFormat.format("Error injecting browser timing footer in a JSP: {0}", new Object[] { t });
      logException(msg, t);
    }return "";
  }

  public String getBrowserTimingFooter()
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    try {
      if (!tx.isStarted()) {
        Agent.LOG.finer("Unable to get browser timing footer in NewRelic API: not running in a transaction");
        return "";
      }
      String footer = null;
      synchronized (tx) {
        footer = tx.getBrowserTransactionState().getBrowserTimingFooter();
      }
      if (Agent.LOG.isLoggable(Level.FINER)) {
        String msg = MessageFormat.format("Got browser timing footer in NewRelic API: {0}", new Object[] { footer });
        Agent.LOG.log(Level.FINER, msg);
      }
      return footer;
    } catch (Throwable t) {
      String msg = MessageFormat.format("Error getting browser timing footer in NewRelic API: {0}", new Object[] { t });
      logException(msg, t);
    }return "";
  }

  public void setUserName(String name)
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    Dispatcher dispatcher = tx.getDispatcher();
    if (dispatcher == null) {
      Agent.LOG.finer(MessageFormat.format("Unable to set the user name to \"{0}\" in NewRelic API - no transaction", new Object[] { name }));

      return;
    }
    if (!dispatcher.isWebTransaction()) {
      Agent.LOG.finer(MessageFormat.format("Unable to set the user name to \"{0}\" in NewRelic API - transaction is not a web transaction", new Object[] { name }));

      return;
    }
    if (Agent.LOG.isLoggable(Level.FINER)) {
      String msg = MessageFormat.format("Attepmting to set user name to \"{0}\" in NewRelic API", new Object[] { name });
      Agent.LOG.finer(msg);
    }
    this.attributeSender.addAttribute("user", name, "setUserName");
  }

  public void setAccountName(String name)
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    Dispatcher dispatcher = tx.getDispatcher();
    if (dispatcher == null) {
      Agent.LOG.finer(MessageFormat.format("Unable to set the account name to \"{0}\" in NewRelic API - no transaction", new Object[] { name }));

      return;
    }
    if (!dispatcher.isWebTransaction()) {
      Agent.LOG.finer(MessageFormat.format("Unable to set the account name to \"{0}\" in NewRelic API - transaction is not a web transaction", new Object[] { name }));

      return;
    }
    if (Agent.LOG.isLoggable(Level.FINER)) {
      String msg = MessageFormat.format("Attepmting to set account name to \"{0}\" in NewRelic API", new Object[] { name });
      Agent.LOG.finer(msg);
    }
    this.attributeSender.addAttribute("account", name, "setAccountName");
  }

  public void setProductName(String name)
  {
    Transaction tx = Transaction.getTransaction().getRootTransaction();
    Dispatcher dispatcher = tx.getDispatcher();
    if (dispatcher == null) {
      Agent.LOG.finer(MessageFormat.format("Unable to set the product name to \"{0}\" in NewRelic API - no transaction", new Object[] { name }));

      return;
    }
    if (!dispatcher.isWebTransaction()) {
      Agent.LOG.finer(MessageFormat.format("Unable to set the product name to \"{0}\" in NewRelic API - transaction is not a web transaction", new Object[] { name }));

      return;
    }
    if (Agent.LOG.isLoggable(Level.FINER)) {
      String msg = MessageFormat.format("Attepmting to set product name to \"{0}\" in NewRelic API", new Object[] { name });
      Agent.LOG.finer(msg);
    }
    this.attributeSender.addAttribute("product", name, "setProductName");
  }

  private static void logException(String msg, Throwable t) {
    if (Agent.LOG.isLoggable(Level.FINEST))
      Agent.LOG.log(Level.FINEST, msg, t);
    else if (Agent.LOG.isLoggable(Level.FINER))
      Agent.LOG.finer(msg);
  }

  public static void initialize()
  {
    com.newrelic.agent.bridge.AgentBridge.publicApi = new NewRelicApiImplementation();
  }
}