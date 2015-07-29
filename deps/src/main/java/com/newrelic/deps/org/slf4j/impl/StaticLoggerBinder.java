/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 * <p/>
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 * <p/>
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 * <p/>
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.newrelic.deps.org.slf4j.impl;

import com.newrelic.deps.ch.qos.logback.classic.LoggerContext;
import com.newrelic.deps.ch.qos.logback.classic.util.ContextInitializer;
import com.newrelic.deps.ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import com.newrelic.deps.ch.qos.logback.core.joran.spi.JoranException;
import com.newrelic.deps.ch.qos.logback.core.status.StatusUtil;
import com.newrelic.deps.ch.qos.logback.core.util.StatusPrinter;
import com.newrelic.deps.org.slf4j.ILoggerFactory;
import com.newrelic.deps.org.slf4j.helpers.Util;
import com.newrelic.deps.org.slf4j.spi.LoggerFactoryBinder;

/**
 * The binding of {@link com.newrelic.deps.org.slf4j.LoggerFactory} class with an actual instance of
 * {@link ILoggerFactory} is performed using information returned by this class.
 *
 * This class is meant to provide a dummy StaticLoggerBinder to the slf4j-api module. 
 * Real implementations are found in  each SLF4J binding project, e.g. slf4j-nop, 
 * slf4j-log4j12 etc.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

    static final String NULL_CS_URL = "http://logback.qos.ch/codes.html#null_CS";
    /**
     * Declare the version of the SLF4J API this implementation is compiled against.
     * The value of this field is usually modified with each release.
     */
    // to avoid constant folding by the compiler, this field must *not* be final
    public static String REQUESTED_API_VERSION = "1.6.99";  // !final
    /**
     * The unique instance of this class.
     */
    private static StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
    private static Object KEY = new Object();

    static {
        SINGLETON.init();
    }

    private final ContextSelectorStaticBinder contextSelectorBinder = ContextSelectorStaticBinder.getSingleton();
    private boolean initialized = false;
    private LoggerContext defaultLoggerContext = new LoggerContext();

    /**
     * Return the singleton of this class.
     *
     * @return the StaticLoggerBinder singleton
     */
    public static final StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    static void reset() {
        SINGLETON = new StaticLoggerBinder();
        SINGLETON.init();
    }

    private StaticLoggerBinder() {
        this.defaultLoggerContext.setName("default");
    }

    void init() {
        try {
            try {
                (new ContextInitializer(this.defaultLoggerContext)).autoConfig();
            } catch (JoranException var2) {
                Util.report("Failed to auto configure default logger context", var2);
            }

            if (!StatusUtil.contextHasStatusListener(this.defaultLoggerContext)) {
                StatusPrinter.printInCaseOfErrorsOrWarnings(this.defaultLoggerContext);
            }

            this.contextSelectorBinder.init(this.defaultLoggerContext, KEY);
            this.initialized = true;
        } catch (Throwable var3) {
            Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", var3);
        }

    }

    public ILoggerFactory getLoggerFactory() {
        if (!this.initialized) {
            return this.defaultLoggerContext;
        } else if (this.contextSelectorBinder.getContextSelector() == null) {
            throw new IllegalStateException("contextSelector cannot be null. See also " + NULL_CS_URL);
        } else {
            return this.contextSelectorBinder.getContextSelector().getLoggerContext();
        }
    }

    public String getLoggerFactoryClassStr() {
        return this.contextSelectorBinder.getClass().getName();
    }
}
