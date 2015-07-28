/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.newrelic.deps.org.apache.http.impl;

import java.util.Locale;

import com.newrelic.deps.org.apache.http.HttpResponse;
import com.newrelic.deps.org.apache.http.HttpResponseFactory;
import com.newrelic.deps.org.apache.http.ProtocolVersion;
import com.newrelic.deps.org.apache.http.ReasonPhraseCatalog;
import com.newrelic.deps.org.apache.http.StatusLine;
import com.newrelic.deps.org.apache.http.annotation.Immutable;
import com.newrelic.deps.org.apache.http.message.BasicHttpResponse;
import com.newrelic.deps.org.apache.http.message.BasicStatusLine;
import com.newrelic.deps.org.apache.http.protocol.HttpContext;
import com.newrelic.deps.org.apache.http.util.Args;

/**
 * Default factory for creating {@link HttpResponse} objects.
 *
 * @since 4.0
 */
@Immutable
public class DefaultHttpResponseFactory implements HttpResponseFactory {

    public static final DefaultHttpResponseFactory INSTANCE = new DefaultHttpResponseFactory();

    /** The catalog for looking up reason phrases. */
    protected final ReasonPhraseCatalog reasonCatalog;


    /**
     * Creates a new response factory with the given catalog.
     *
     * @param catalog   the catalog of reason phrases
     */
    public DefaultHttpResponseFactory(final ReasonPhraseCatalog catalog) {
        this.reasonCatalog = Args.notNull(catalog, "Reason phrase catalog");
    }

    /**
     * Creates a new response factory with the default catalog.
     * The default catalog is {@link EnglishReasonPhraseCatalog}.
     */
    public DefaultHttpResponseFactory() {
        this(EnglishReasonPhraseCatalog.INSTANCE);
    }


    // non-javadoc, see interface HttpResponseFactory
    public HttpResponse newHttpResponse(
            final ProtocolVersion ver,
            final int status,
            final HttpContext context) {
        Args.notNull(ver, "HTTP version");
        final Locale loc = determineLocale(context);
        final String reason   = reasonCatalog.getReason(status, loc);
        final StatusLine statusline = new BasicStatusLine(ver, status, reason);
        return new BasicHttpResponse(statusline);
    }


    // non-javadoc, see interface HttpResponseFactory
    public HttpResponse newHttpResponse(
            final StatusLine statusline,
            final HttpContext context) {
        Args.notNull(statusline, "Status line");
        return new BasicHttpResponse(statusline);
    }

    /**
     * Determines the locale of the response.
     * The implementation in this class always returns the default locale.
     *
     * @param context   the context from which to determine the locale, or
     *                  <code>null</code> to use the default locale
     *
     * @return  the locale for the response, never <code>null</code>
     */
    protected Locale determineLocale(final HttpContext context) {
        return Locale.getDefault();
    }

}
