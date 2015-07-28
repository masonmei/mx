/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package com.newrelic.deps.ch.qos.logback.core.db;
import java.sql.Connection;
import java.sql.SQLException;

import com.newrelic.deps.ch.qos.logback.core.db.dialect.SQLDialectCode;
import com.newrelic.deps.ch.qos.logback.core.spi.LifeCycle;


/**
 *  The <id>ConnectionSource</id> interface provides a pluggable means of
 *  transparently obtaining JDBC {@link Connection}s for logback classes
 *  that require the use of a {@link Connection}.
 *  
 * For more information about this component, please refer to the online manual at
 * http://logback.qos.ch/manual/appenders.html#DBAppender
 *
 *  @author <a href="mailto:rdecampo@twcny.rr.com">Ray DeCampo</a>
 */
public interface ConnectionSource extends LifeCycle {

  /**
   *  Obtain a {@link Connection} for use.  The client is
   *  responsible for closing the {@link Connection} when it is no
   *  longer required.
   *
   *  @throws SQLException  if a {@link Connection} could not be
   *                        obtained
   */
  Connection getConnection() throws SQLException;

  /**
   * Get the SQL dialect that should be used for this connection. Note that the
   * dialect is not needed if the JDBC driver supports the getGeneratedKeys 
   * method.
   */
  SQLDialectCode getSQLDialectCode();
  
  /**
   * If the connection supports the JDBC 3.0 getGeneratedKeys method, then
   * we do not need any specific dialect support.
   */
  boolean supportsGetGeneratedKeys();
  
  /**
   * If the connection does not support batch updates, we will avoid using them.
   */
  boolean supportsBatchUpdates();
}
