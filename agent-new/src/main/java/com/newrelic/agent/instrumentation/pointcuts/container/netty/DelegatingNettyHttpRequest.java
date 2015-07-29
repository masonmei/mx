//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.instrumentation.pointcuts.container.netty;

import java.net.HttpCookie;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.Agent;
import com.newrelic.agent.util.IteratorEnumeration;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Request;

public class DelegatingNettyHttpRequest implements Request {
  public static final String COOKIE_HEADER_NAME = "Cookie";
  private final NettyHttpRequest delegate;
  private volatile Map<String, String> cookies;
  private volatile Map<String, List<Object>> parameters;

  private DelegatingNettyHttpRequest(NettyHttpRequest delegate) {
    this.delegate = delegate;
  }

  static Request create(NettyHttpRequest delegate) {
    return new DelegatingNettyHttpRequest(delegate);
  }

  public HeaderType getHeaderType() {
    return HeaderType.HTTP;
  }

  private Map<String, String> getCookies() {
    if (this.cookies == null) {
      this.cookies = new HashMap();
      List cookieHeaders = this.delegate.getHeaders(COOKIE_HEADER_NAME);
      Iterator i$ = cookieHeaders.iterator();

      while (i$.hasNext()) {
        String cookieHeader = (String) i$.next();

        Object httpCookies;
        try {
          httpCookies = HttpCookie.parse(cookieHeader);
        } catch (IllegalArgumentException var12) {
          httpCookies = new LinkedList();
          String[] httpCookie = cookieHeader.split(";");
          int len$ = httpCookie.length;

          for (int i$2 = 0; i$2 < len$; ++i$2) {
            String part = httpCookie[i$2];

            try {
              ((List) httpCookies).addAll(HttpCookie.parse(part));
            } catch (IllegalArgumentException var11) {
              Agent.LOG.fine("Failed to parse Cookie part: " + part);
            }
          }
        }

        Iterator i$1 = ((List) httpCookies).iterator();

        while (i$1.hasNext()) {
          HttpCookie var13 = (HttpCookie) i$1.next();
          this.cookies.put(var13.getName(), var13.getValue());
        }
      }
    }

    return this.cookies;
  }

  public void setParameters(Map<String, List<Object>> params) {
    this.parameters = params;
  }

  public Enumeration<?> getParameterNames() {
    if (this.parameters == null) {
      return null;
    } else {
      Iterator it = this.parameters.keySet().iterator();
      return new IteratorEnumeration(it);
    }
  }

  public String[] getParameterValues(String name) {
    return this.parameters == null
                   ? null
                   : (this.parameters.get(name) == null
                              ? null
                              : (String[]) ((List) this.parameters.get(name))
                                                   .toArray(new String[((List) this.parameters
                                                                                       .get(name))
                                                                               .size()]));
  }

  public Object getAttribute(String name) {
    return null;
  }

  public String getRequestURI() {
    return this.delegate.getUri();
  }

  public String getHeader(String name) {
    List nameHeaders = this.delegate.getHeaders(name);
    return nameHeaders != null && nameHeaders.size() > 0 ? (String) nameHeaders.get(0) : null;
  }

  public String getRemoteUser() {
    return null;
  }

  public String getCookieValue(String name) {
    Map map = this.getCookies();
    return map == null ? null : (String) map.get(name);
  }
}
