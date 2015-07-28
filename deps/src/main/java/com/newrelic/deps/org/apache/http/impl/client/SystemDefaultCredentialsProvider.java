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
package com.newrelic.deps.org.apache.http.impl.client;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.newrelic.deps.org.apache.http.annotation.ThreadSafe;
import com.newrelic.deps.org.apache.http.auth.AuthScope;
import com.newrelic.deps.org.apache.http.auth.Credentials;
import com.newrelic.deps.org.apache.http.auth.UsernamePasswordCredentials;
import com.newrelic.deps.org.apache.http.client.CredentialsProvider;
import com.newrelic.deps.org.apache.http.client.config.AuthSchemes;
import com.newrelic.deps.org.apache.http.util.Args;

/**
 * Implementation of {@link CredentialsProvider} backed by standard
 * JRE {@link Authenticator}.
 *
 * @since 4.3
 */
@ThreadSafe
public class SystemDefaultCredentialsProvider implements CredentialsProvider {

    private static final Map<String, String> SCHEME_MAP;

    static {
        SCHEME_MAP = new ConcurrentHashMap<String, String>();
        SCHEME_MAP.put(AuthSchemes.BASIC.toUpperCase(Locale.ENGLISH), "Basic");
        SCHEME_MAP.put(AuthSchemes.DIGEST.toUpperCase(Locale.ENGLISH), "Digest");
        SCHEME_MAP.put(AuthSchemes.NTLM.toUpperCase(Locale.ENGLISH), "NTLM");
        SCHEME_MAP.put(AuthSchemes.SPNEGO.toUpperCase(Locale.ENGLISH), "SPNEGO");
        SCHEME_MAP.put(AuthSchemes.KERBEROS.toUpperCase(Locale.ENGLISH), "Kerberos");
    }

    private static String translateScheme(final String key) {
        if (key == null) {
            return null;
        }
        final String s = SCHEME_MAP.get(key);
        return s != null ? s : key;
    }

    private final BasicCredentialsProvider internal;

    /**
     * Default constructor.
     */
    public SystemDefaultCredentialsProvider() {
        super();
        this.internal = new BasicCredentialsProvider();
    }

    public void setCredentials(final AuthScope authscope, final Credentials credentials) {
        internal.setCredentials(authscope, credentials);
    }

    public Credentials getCredentials(final AuthScope authscope) {
        Args.notNull(authscope, "Auth scope");
        final Credentials localcreds = internal.getCredentials(authscope);
        if (localcreds != null) {
            return localcreds;
        }
        if (authscope.getHost() != null) {
            final PasswordAuthentication systemcreds = Authenticator.requestPasswordAuthentication(
                    authscope.getHost(),
                    null,
                    authscope.getPort(),
                    "http",
                    null,
                    translateScheme(authscope.getScheme()));
            if (systemcreds != null) {
                return new UsernamePasswordCredentials(
                        systemcreds.getUserName(), new String(systemcreds.getPassword()));
            }
        }
        return null;
    }

    public void clear() {
        internal.clear();
    }

}
