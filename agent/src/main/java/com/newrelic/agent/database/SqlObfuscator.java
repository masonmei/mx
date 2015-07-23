//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.database;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class SqlObfuscator {
    public static final String OBFUSCATED_SETTING = "obfuscated";
    public static final String RAW_SETTING = "raw";
    public static final String OFF_SETTING = "off";

    private SqlObfuscator() {
    }

    public static SqlObfuscator getDefaultSqlObfuscator() {
        return new SqlObfuscator.DefaultSqlObfuscator();
    }

    static SqlObfuscator getNoObfuscationSqlObfuscator() {
        return new SqlObfuscator() {
            public String obfuscateSql(String sql) {
                return sql;
            }
        };
    }

    static SqlObfuscator getNoSqlObfuscator() {
        return new SqlObfuscator() {
            public String obfuscateSql(String sql) {
                return null;
            }
        };
    }

    public static SqlObfuscator getCachingSqlObfuscator(SqlObfuscator sqlObfuscator) {
        return (SqlObfuscator) (sqlObfuscator.isObfuscating() ? new SqlObfuscator.CachingSqlObfuscator(sqlObfuscator)
                                        : sqlObfuscator);
    }

    public abstract String obfuscateSql(String var1);

    public boolean isObfuscating() {
        return false;
    }

    static class CachingSqlObfuscator extends SqlObfuscator {
        private final Map<String, String> cache = new HashMap();
        private final SqlObfuscator sqlObfuscator;

        public CachingSqlObfuscator(SqlObfuscator sqlObfuscator) {
            super();
            this.sqlObfuscator = sqlObfuscator;
        }

        public String obfuscateSql(String sql) {
            String obfuscatedSql = (String) this.cache.get(sql);
            if (obfuscatedSql == null) {
                obfuscatedSql = this.sqlObfuscator.obfuscateSql(sql);
                this.cache.put(sql, obfuscatedSql);
            }

            return obfuscatedSql;
        }

        public boolean isObfuscating() {
            return this.sqlObfuscator.isObfuscating();
        }
    }

    static class DefaultSqlObfuscator extends SqlObfuscator {
        protected static final Pattern[] OBFUSCATION_PATTERNS;
        private static final Pattern DIGIT_PATTERN =
                Pattern.compile("(?<=[-+*/,_<=>)(\\.\\s])\\d+(?=[-+*/,_<=>)(\\.\\s]|$)");

        static {
            OBFUSCATION_PATTERNS = new Pattern[] {Pattern.compile("\'(.*?[^\\\'])??\'(?!\')", 32),
                                                         Pattern.compile("\"(.*?[^\\\"])??\"(?!\")", 32),
                                                         DIGIT_PATTERN};
        }

        DefaultSqlObfuscator() {
            super();
        }

        public String obfuscateSql(String sql) {
            if (sql != null && sql.length() != 0) {
                Pattern[] arr$ = OBFUSCATION_PATTERNS;
                int len$ = arr$.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    Pattern pattern = arr$[i$];
                    sql = pattern.matcher(sql).replaceAll("?");
                }

                return sql;
            } else {
                return sql;
            }
        }

        public boolean isObfuscating() {
            return true;
        }
    }
}
