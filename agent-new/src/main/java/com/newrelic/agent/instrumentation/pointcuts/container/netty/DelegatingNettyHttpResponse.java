package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;

public class DelegatingNettyHttpResponse
  implements Response
{
  private volatile NettyHttpResponse delegate;

  private DelegatingNettyHttpResponse(NettyHttpResponse delegate)
  {
    this.delegate = delegate;
  }

  public void setDelegate(NettyHttpResponse delegate) {
    this.delegate = delegate;
  }

  static Response create(NettyHttpResponse delegate) {
    return new DelegatingNettyHttpResponse(delegate);
  }

  public HeaderType getHeaderType()
  {
    return HeaderType.HTTP;
  }

  public int getStatus() throws Exception
  {
    return this.delegate == null ? 0 : this.delegate._nr_status().getCode();
  }

  public String getStatusMessage() throws Exception
  {
    return this.delegate == null ? "" : this.delegate._nr_status().getReasonPhrase();
  }

  public String getContentType()
  {
    return null;
  }

  public void setHeader(String name, String value)
  {
    if (this.delegate == null) {
      return;
    }
    this.delegate.setHeader(name, value);
  }
}