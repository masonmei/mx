//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.database;

import java.sql.ResultSetMetaData;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.util.Strings;

public class DefaultDatabaseStatementParser implements DatabaseStatementParser {
    private static final int PATTERN_SWITCHES = 34;
    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", 32);
    private static final Pattern NR_HINT_PATTERN =
            Pattern.compile("\\s*/\\*\\s*nrhint\\s*:\\s*([^\\*]*)\\s*\\*/\\s*([^\\s]*).*", 32);
    private static final Pattern VALID_METRIC_NAME_MATCHER = Pattern.compile("[a-zA-z0-9.\\$]*");
    private static final Pattern FROM_MATCHER = Pattern.compile("\\s+from\\s+", PATTERN_SWITCHES);
    private static final Pattern SELECT_PATTERN =
            Pattern.compile("^\\s*select.*?\\sfrom[\\s\\[]+([^\\]\\s,)(;]*).*", PATTERN_SWITCHES);
    private final Set<String> knownOperations;
    private final List<DefaultDatabaseStatementParser.StatementFactory> statementFactories;
    private final boolean reportSqlParserErrors;
    private final DefaultDatabaseStatementParser.StatementFactory selectStatementFactory;

    public DefaultDatabaseStatementParser(AgentConfig agentConfig) {
        this.selectStatementFactory =
                new DefaultDatabaseStatementParser.DefaultStatementFactory("select", SELECT_PATTERN, true);
        this.reportSqlParserErrors = agentConfig.isReportSqlParserErrors();
        this.statementFactories =
                Arrays.asList(new InnerSelectStatementFactory(),
                                     new DefaultStatementFactory("show", Pattern.compile("^\\s*show\\s+(.*)$", PATTERN_SWITCHES), false) {
                                         protected boolean isValidModelName(String name) {
                                             return true;
                                         }
                                     },
                                     new DefaultStatementFactory("insert", Pattern.compile("^\\s*insert(?:\\s+ignore)?\\s+into\\s+([^\\s(,;]*).*", PATTERN_SWITCHES), true),
                                     new DefaultStatementFactory("update", Pattern.compile("^\\s*update\\s+([^\\s,;]*).*", PATTERN_SWITCHES), true),
                                     new DefaultStatementFactory("delete", Pattern.compile("^\\s*delete\\s+from\\s+([^\\s,(;]*).*", PATTERN_SWITCHES), true),
                                     new DDLStatementFactory("create", Pattern.compile("^\\s*create\\s+procedure.*", PATTERN_SWITCHES), "Procedure"),
                                     new SelectVariableStatementFactory(),
                                     new DDLStatementFactory("drop", Pattern.compile("^\\s*drop\\s+procedure.*", PATTERN_SWITCHES), "Procedure"),
                                     new DDLStatementFactory("create", Pattern.compile("^\\s*create\\s+table.*", PATTERN_SWITCHES), "Table"),
                                     new DDLStatementFactory("drop", Pattern.compile("^\\s*drop\\s+table.*", PATTERN_SWITCHES), "Table"),
                                     new DefaultStatementFactory("alter", Pattern.compile("^\\s*alter\\s+([^\\s]*).*", PATTERN_SWITCHES), false),
                                     new DefaultStatementFactory("call", Pattern.compile(".*call\\s+([^\\s(,]*).*", PATTERN_SWITCHES), true),
                                     new DefaultStatementFactory("exec", Pattern.compile(".*(?:exec|execute)\\s+([^\\s(,]*).*", PATTERN_SWITCHES), true),
                                     new DefaultStatementFactory("set", Pattern.compile("^\\s*set\\s+(.*)\\s*(as|=).*", PATTERN_SWITCHES), false));
        this.knownOperations = new HashSet();
        Iterator i$ = this.statementFactories.iterator();

        while (i$.hasNext()) {
            DefaultDatabaseStatementParser.StatementFactory factory =
                    (DefaultDatabaseStatementParser.StatementFactory) i$.next();
            this.knownOperations.add(factory.getOperation());
        }

    }

    static boolean isValidName(String string) {
        return VALID_METRIC_NAME_MATCHER.matcher(string).matches();
    }

    public ParsedDatabaseStatement getParsedDatabaseStatement(String statement, ResultSetMetaData metaData) {
        Matcher hintMatcher = NR_HINT_PATTERN.matcher(statement);
        String tableName;
        if (hintMatcher.matches()) {
            String e1 = hintMatcher.group(1).trim().toLowerCase();
            tableName = hintMatcher.group(2).toLowerCase();
            if (!this.knownOperations.contains(tableName)) {
                tableName = "unknown";
            }

            return new ParsedDatabaseStatement(e1, tableName, true);
        } else {
            if (metaData != null) {
                try {
                    int e = metaData.getColumnCount();
                    if (e > 0) {
                        tableName = metaData.getTableName(1);
                        if (!Strings.isEmpty(tableName)) {
                            return new ParsedDatabaseStatement(tableName.toLowerCase(), "select", true);
                        }
                    }
                } catch (Exception var6) {
                    ;
                }
            }

            return this.parseStatement(statement);
        }
    }

    ParsedDatabaseStatement parseStatement(String statement) {
        try {
            statement = COMMENT_PATTERN.matcher(statement).replaceAll("");
            Iterator t = this.statementFactories.iterator();

            ParsedDatabaseStatement parsedStatement;
            do {
                if (!t.hasNext()) {
                    return UNPARSEABLE_STATEMENT;
                }

                DefaultDatabaseStatementParser.StatementFactory factory =
                        (DefaultDatabaseStatementParser.StatementFactory) t.next();
                parsedStatement = factory.parseStatement(statement);
            } while (parsedStatement == null);

            return parsedStatement;
        } catch (Throwable var5) {
            Agent.LOG.fine(MessageFormat.format("Unable to parse sql \"{0}\" - {1}", statement, var5.toString()));
            Agent.LOG.log(Level.FINER, "SQL parsing error", var5);
            return UNPARSEABLE_STATEMENT;
        }
    }

    private interface StatementFactory {
        String getOperation();

        ParsedDatabaseStatement parseStatement(String var1);
    }

    private class DDLStatementFactory extends DefaultDatabaseStatementParser.DefaultStatementFactory {
        private final String type;

        public DDLStatementFactory(String key, Pattern pattern, String type) {
            super(key, pattern, true);
            this.type = type;
        }

        ParsedDatabaseStatement createParsedDatabaseStatement(String model) {
            return new ParsedDatabaseStatement(this.type, this.key, this.isMetricGenerator());
        }
    }

    private class InnerSelectStatementFactory implements DefaultDatabaseStatementParser.StatementFactory {
        private final Pattern innerSelectPattern;

        private InnerSelectStatementFactory() {
            this.innerSelectPattern = Pattern.compile("^\\s*SELECT.*?\\sFROM\\s*\\(\\s*(SELECT.*)", 34);
        }

        public ParsedDatabaseStatement parseStatement(String statement) {
            String sql = statement;
            String res = null;

            while (true) {
                String res2 = this.findMatch(sql);
                if (res2 == null) {
                    return res != null ? DefaultDatabaseStatementParser.this.selectStatementFactory.parseStatement(res)
                                   : DefaultDatabaseStatementParser.this.selectStatementFactory
                                             .parseStatement(statement);
                }

                res = res2;
                sql = res2;
            }
        }

        private String findMatch(String statement) {
            Matcher matcher = this.innerSelectPattern.matcher(statement);
            return matcher.matches() ? matcher.group(1) : null;
        }

        public String getOperation() {
            return "select";
        }
    }

    private class SelectVariableStatementFactory implements DefaultDatabaseStatementParser.StatementFactory {
        private final ParsedDatabaseStatement innerSelectStatement;
        private final ParsedDatabaseStatement statement;
        private final Pattern pattern;

        private SelectVariableStatementFactory() {
            this.innerSelectStatement = new ParsedDatabaseStatement("INNER_SELECT", "select", false);
            this.statement = new ParsedDatabaseStatement("VARIABLE", "select", false);
            this.pattern = Pattern.compile(".*select\\s+([^\\s,]*).*", PATTERN_SWITCHES);
        }

        public ParsedDatabaseStatement parseStatement(String statement) {
            Matcher matcher = this.pattern.matcher(statement);
            return matcher.matches() ? (DefaultDatabaseStatementParser.FROM_MATCHER.matcher(statement).find()
                                                ? this.innerSelectStatement : this.statement) : null;
        }

        public String getOperation() {
            return "select";
        }
    }

    class DefaultStatementFactory implements DefaultDatabaseStatementParser.StatementFactory {
        protected final String key;
        private final Pattern pattern;
        private final boolean generateMetric;

        public DefaultStatementFactory(String key, Pattern pattern, boolean generateMetric) {
            this.key = key;
            this.pattern = pattern;
            this.generateMetric = generateMetric;
        }

        protected boolean isMetricGenerator() {
            return this.generateMetric;
        }

        public ParsedDatabaseStatement parseStatement(String statement) {
            Matcher matcher = this.pattern.matcher(statement);
            if (matcher.matches()) {
                String model = matcher.groupCount() > 0 ? matcher.group(1).trim() : "unknown";
                if (model.length() == 0) {
                    Agent.LOG.log(Level.FINE, MessageFormat.format("Parsed an empty model name for {0} statement : {1}",
                                                                          this.key, statement));
                    return null;
                } else {
                    model = Strings.unquoteDatabaseName(model);
                    if (this.generateMetric && !this.isValidModelName(model)) {
                        if (DefaultDatabaseStatementParser.this.reportSqlParserErrors) {
                            Agent.LOG.log(Level.FINE, MessageFormat.format("Parsed an invalid model name {0} for {1} "
                                                                                   + "statement : {2}", model, this.key,
                                                                                  statement));
                        }

                        model = "ParseError";
                    }

                    return this.createParsedDatabaseStatement(model);
                }
            } else {
                return null;
            }
        }

        protected boolean isValidModelName(String name) {
            return DefaultDatabaseStatementParser.isValidName(name);
        }

        ParsedDatabaseStatement createParsedDatabaseStatement(String model) {
            return new ParsedDatabaseStatement(model.toLowerCase(), this.key, this.generateMetric);
        }

        public String getOperation() {
            return this.key;
        }
    }
}
