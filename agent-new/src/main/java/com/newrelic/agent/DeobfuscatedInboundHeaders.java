package com.newrelic.agent;

import com.newrelic.agent.util.Obfuscator;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import java.io.UnsupportedEncodingException;
import java.util.Set;

public class DeobfuscatedInboundHeaders
  implements InboundHeaders
{
  InboundHeaders delegate;
  String encodingKey;

  public DeobfuscatedInboundHeaders(InboundHeaders headers, String encodingKey)
  {
    this.delegate = headers;
    this.encodingKey = encodingKey;
  }

  public HeaderType getHeaderType()
  {
    return this.delegate.getHeaderType();
  }

  public String getHeader(String name)
  {
    if (this.encodingKey == null) {
      return null;
    }

    if (HeadersUtil.NEWRELIC_HEADERS.contains(name)) {
      String obfuscatedValue = this.delegate.getHeader(name);
      if (obfuscatedValue == null)
        return null;
      try
      {
        return Obfuscator.deobfuscateNameUsingKey(obfuscatedValue, this.encodingKey);
      } catch (UnsupportedEncodingException e) {
        return null;
      }
    }

    return this.delegate.getHeader(name);
  }
}