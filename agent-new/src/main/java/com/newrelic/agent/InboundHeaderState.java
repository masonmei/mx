package com.newrelic.agent;

import com.newrelic.agent.config.CrossProcessConfig;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.parser.JSONParser;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.api.agent.InboundHeaders;
import java.text.MessageFormat;
import java.util.logging.Level;

public class InboundHeaderState
{
  private static final String CONTENT_LENGTH_REQUEST_HEADER = "Content-Length";
  private static final String NEWRELIC_ID_HEADER_SEPARATOR = "#";
  private static final int CURRENT_SYNTHETICS_VERSION = 1;
  private final Transaction tx;
  private final InboundHeaders inboundHeaders;
  private final CatState catState;
  private final SyntheticsState synState;

  public InboundHeaderState(Transaction tx, InboundHeaders inboundHeaders)
  {
    this.tx = tx;
    this.inboundHeaders = inboundHeaders;

    if (inboundHeaders == null) {
      this.synState = SyntheticsState.NONE;
      this.catState = CatState.NONE;
    } else {
      this.synState = parseSyntheticsHeader();
      this.catState = parseCatHeaders();
    }
  }

  public String getUnparsedSyntheticsHeader()
  {
    String result = null;

    if (this.inboundHeaders != null) {
      result = HeadersUtil.getSyntheticsHeader(this.inboundHeaders);
    }
    return result;
  }

  private SyntheticsState parseSyntheticsHeader() {
    String synHeader = getUnparsedSyntheticsHeader();
    if ((synHeader == null) || (synHeader.length() == 0)) {
      return SyntheticsState.NONE;
    }

    JSONArray arr = getJSONArray(synHeader);
    if ((arr == null) || (arr.size() == 0)) {
      Agent.LOG.fine("Synthetic transaction tracing failed: unable to decode header.");
      return SyntheticsState.NONE;
    }

    Agent.LOG.log(Level.FINEST, "Decoded synthetics header => {0}", new Object[] { arr });

    Integer version = null;
    try {
      version = Integer.valueOf(Integer.parseInt(arr.get(0).toString()));
    } catch (NumberFormatException nfe) {
      Agent.LOG.log(Level.FINEST, "Could not determine Synetics version. Value => {0}. Class => {1}.", new Object[] { arr.get(0), arr.get(0).getClass() });

      return SyntheticsState.NONE;
    }

    if (version.intValue() > 1) {
      Agent.LOG.log(Level.FINE, "Synthetic transaction tracing failed: invalid version {0}", new Object[] { version });
      return SyntheticsState.NONE;
    }

    SyntheticsState result;
    try
    {
      result = new SyntheticsState(version, (Number)arr.get(1), (String)arr.get(2), (String)arr.get(3), (String)arr.get(4));
    }
    catch (RuntimeException rex) {
      Agent.LOG.log(Level.FINE, "Synthetic transaction tracing failed: while parsing header: {0}: {1}", new Object[] { rex.getClass().getSimpleName(), rex.getLocalizedMessage() });

      result = SyntheticsState.NONE;
    }

    return result;
  }

  private CatState parseCatHeaders() {
    String clientCrossProcessID = HeadersUtil.getIdHeader(this.inboundHeaders);
    if ((clientCrossProcessID == null) || (this.tx.isIgnore())) {
      return CatState.NONE;
    }

    if (!this.tx.getCrossProcessConfig().isCrossApplicationTracing()) {
      return CatState.NONE;
    }

    if (!isClientCrossProcessIdTrusted(clientCrossProcessID)) {
      return CatState.NONE;
    }

    if (Agent.LOG.isFinestEnabled()) {
      String msg = MessageFormat.format("Client cross process id is {0}", new Object[] { clientCrossProcessID });
      Agent.LOG.finest(msg);
    }

    String transactionHeader = HeadersUtil.getTransactionHeader(this.inboundHeaders);
    JSONArray arr = getJSONArray(transactionHeader);
    if (arr == null) {
      return new CatState(clientCrossProcessID, null, Boolean.FALSE, null, null);
    }

    return new CatState(clientCrossProcessID, arr.size() >= 1 ? (String)arr.get(0) : null, arr.size() >= 2 ? (Boolean)arr.get(1) : null, arr.size() >= 3 ? (String)arr.get(2) : null, arr.size() >= 4 ? Integer.valueOf(ServiceUtils.hexStringToInt((String)arr.get(3))) : null);
  }

  public int getSyntheticsVersion()
  {
    Integer obj = this.synState.getVersion();
    if (obj == null) {
      return -1;
    }

    int version = obj.intValue();
    if (version < 0)
    {
      return -1;
    }

    return version;
  }

  private boolean isSupportedSyntheticsVersion()
  {
    int version = getSyntheticsVersion();
    return (version >= 1) && (version <= 1);
  }

  public boolean isTrustedSyntheticsRequest()
  {
    return (isSupportedSyntheticsVersion()) && (this.synState.getAccountId() != null);
  }

  public String getSyntheticsResourceId() {
    return this.synState.getSyntheticsResourceId();
  }

  public String getSyntheticsJobId() {
    return this.synState.getSyntheticsJobId();
  }

  public String getSyntheticsMonitorId() {
    return this.synState.getSyntheticsMonitorId();
  }

  public boolean isTrustedCatRequest()
  {
    return this.catState.getClientCrossProcessId() != null;
  }

  public String getClientCrossProcessId() {
    return this.catState.getClientCrossProcessId();
  }

  public String getReferrerGuid() {
    return this.catState.getReferrerGuid();
  }

  public boolean forceTrace() {
    return this.catState.forceTrace();
  }

  public Integer getReferringPathHash() {
    return this.catState.getReferringPathHash();
  }

  public String getInboundTripId() {
    return this.catState.getInboundTripId();
  }

  public long getRequestContentLength()
  {
    long contentLength = -1L;
    String contentLengthString = this.inboundHeaders == null ? null : this.inboundHeaders.getHeader("Content-Length");

    if (contentLengthString != null) {
      try {
        contentLength = Long.parseLong(contentLengthString);
      } catch (NumberFormatException e) {
        String msg = MessageFormat.format("Error parsing {0} response header: {1}: {2}", new Object[] { "Content-Length", contentLengthString, e });

        Agent.LOG.finer(msg);
      }
    }
    return contentLength;
  }

  private boolean isClientCrossProcessIdTrusted(String clientCrossProcessId) {
    String accountId = getAccountId(clientCrossProcessId);
    if (accountId != null) {
      if (this.tx.getCrossProcessConfig().isTrustedAccountId(accountId)) {
        return true;
      }
      if (Agent.LOG.isLoggable(Level.FINEST)) {
        String msg = MessageFormat.format("Account id {0} in client cross process id {1} is not trusted", new Object[] { accountId, clientCrossProcessId });

        Agent.LOG.log(Level.FINEST, msg);
      }
    }
    else if (Agent.LOG.isLoggable(Level.FINER)) {
      String msg = MessageFormat.format("Account id not found in client cross process id {0}", new Object[] { clientCrossProcessId });

      Agent.LOG.log(Level.FINER, msg);
    }

    return false;
  }

  private String getAccountId(String clientCrossProcessId)
  {
    String accountId = null;
    int index = clientCrossProcessId.indexOf("#");
    if (index > 0) {
      accountId = clientCrossProcessId.substring(0, index);
    }
    return accountId;
  }

  private JSONArray getJSONArray(String json) {
    JSONArray result = null;
    if (json != null) {
      try {
        JSONParser parser = new JSONParser();
        result = (JSONArray)parser.parse(json);
      } catch (Exception ex) {
        if (Agent.LOG.isFinerEnabled()) {
          Agent.LOG.log(Level.FINER, "Unable to parse TRANSACTION header {0}: {1}", new Object[] { "FIXME", ex });
        }
      }
    }

    return result;
  }

  static final class SyntheticsState
  {
    private final Integer version;
    private final Number accountId;
    private final String syntheticsResourceId;
    private final String syntheticsJobId;
    private final String syntheticsMonitorId;
    static final SyntheticsState NONE = new SyntheticsState(null, null, null, null, null);

    SyntheticsState(Integer version, Number accountId, String syntheticsResourceId, String syntheticsJobId, String syntheticsMonitorId)
    {
      this.version = version;
      this.accountId = accountId;
      this.syntheticsResourceId = syntheticsResourceId;
      this.syntheticsJobId = syntheticsJobId;
      this.syntheticsMonitorId = syntheticsMonitorId;
    }

    Integer getVersion() {
      return this.version;
    }

    Number getAccountId() {
      return this.accountId;
    }

    String getSyntheticsResourceId() {
      return this.syntheticsResourceId;
    }

    String getSyntheticsJobId() {
      return this.syntheticsJobId;
    }

    String getSyntheticsMonitorId() {
      return this.syntheticsMonitorId;
    }
  }

  static final class CatState
  {
    private final String clientCrossProcessId;
    private final String referrerGuid;
    private final Boolean forceTrace;
    private final String inboundTripId;
    private final Integer referringPathHash;
    static final CatState NONE = new CatState(null, null, Boolean.FALSE, null, null);

    CatState(String clientCrossProcessId, String referrerGuid, Boolean forceTrace, String inboundTripId, Integer referringPathHash)
    {
      this.clientCrossProcessId = clientCrossProcessId;
      this.referrerGuid = referrerGuid;
      this.forceTrace = forceTrace;
      this.inboundTripId = inboundTripId;
      this.referringPathHash = referringPathHash;
    }

    String getClientCrossProcessId()
    {
      return this.clientCrossProcessId;
    }

    String getReferrerGuid() {
      return this.referrerGuid;
    }

    boolean forceTrace() {
      return this.forceTrace.booleanValue();
    }

    String getInboundTripId() {
      return this.inboundTripId;
    }

    Integer getReferringPathHash() {
      return this.referringPathHash;
    }
  }
}