package com.newrelic.agent.sql;

import com.newrelic.agent.database.DatabaseService;
import com.newrelic.agent.database.SqlObfuscator;
import com.newrelic.agent.service.ServiceFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObfuscatorUtil
{
  private static final Pattern IN_CLAUSE_PATTERN = Pattern.compile("\\([?,\\s]*\\)");
  private static final String IN_CLAUSE_REPLACEMENT = "(?)";

  public static String obfuscateSql(String sql)
  {
    SqlObfuscator sqlObfuscator = ServiceFactory.getDatabaseService().getDefaultSqlObfuscator();
    return obfuscateInClauses(sqlObfuscator.obfuscateSql(sql));
  }

  private static String obfuscateInClauses(String sql) {
    return IN_CLAUSE_PATTERN.matcher(sql).replaceAll("(?)");
  }
}