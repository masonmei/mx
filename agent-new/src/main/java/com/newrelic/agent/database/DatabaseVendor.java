package com.newrelic.agent.database;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.tracers.metricname.SimpleMetricNameFormat;

public enum DatabaseVendor {
    MYSQL("MySQL", "mysql", true, "^jdbc:mysql://([^/]*)/([^/\\?]*).*"),

    ORACLE("Oracle", "oracle", false, "^jdbc:oracle:(thin|oci):(@//|@)([^:]*:\\d+)(/|:)(.*)"),

    MICROSOFT("Microsoft SQL Server", "sqlserver", false, "^jdbc:sqlserver://([^;]*).*"),

    POSTGRES("PostgreSQL", "postgresql", true, "^jdbc:postgresql://([^/]*)/([^\\?]*).*"),

    DB2("DB2", "db2", false, "jdbc:db2://server:port/database"),

    DERBY("Apache Derby", "derby", false, "^$"), UNKNOWN("Unknown", null, false, "^$");

    public static final MetricNameFormat UNKNOWN_DATABASE_METRIC_NAME;
    static final Pattern POSTGRES_URL_PATTERN;
    static final Pattern POSTGRES_URL_PATTERN_DB;
    private static final String UNKNOWN_STRING = "Unknown";
    private static final String REMOTE_SERVICE_DATABASE_METRIC_NAME = "RemoteService/Database/{0}/{1}/{2}/{3}/all";
    private static final Pattern SIMPLE_DB_URL;
    private static final Pattern TYPE_PATTERN;
    private static final Map<String, DatabaseVendor> TYPE_TO_VENDOR;

    static {
        UNKNOWN_DATABASE_METRIC_NAME = new SimpleMetricNameFormat(MessageFormat
                                                                          .format("RemoteService/Database/{0}/{1}/{2}/{3}/all",
                                                                                         new Object[] {"Unknown",
                                                                                                              "Unknown",
                                                                                                              "Unknown",
                                                                                                              "Unknown"}));

        POSTGRES_URL_PATTERN = Pattern.compile("^jdbc:postgresql://([^/]*).*");

        POSTGRES_URL_PATTERN_DB = Pattern.compile("^jdbc:postgresql:(.*)");

        SIMPLE_DB_URL = Pattern.compile("jdbc:([^:]*):([^/;:].*)");
        TYPE_PATTERN = Pattern.compile("jdbc:([^:]*).*");

        TYPE_TO_VENDOR = new HashMap(7);

        for (DatabaseVendor vendor : values()) {
            TYPE_TO_VENDOR.put(vendor.getType(), vendor);
        }
    }

    final boolean explainPlanSupported;
    final Pattern urlPattern;
    final String type;
    private final String name;

    private DatabaseVendor(String name, String type, boolean explainSupported, String urlPattern) {
        this.name = name;
        this.explainPlanSupported = explainSupported;
        this.type = type;
        this.urlPattern = Pattern.compile(urlPattern);
    }

    public static DatabaseVendor getDatabaseVendor(String url) {
        Matcher matcher = TYPE_PATTERN.matcher(url);
        if (matcher.matches()) {
            String type = matcher.group(1);
            if (type != null) {
                DatabaseVendor vendor = (DatabaseVendor) TYPE_TO_VENDOR.get(type);
                if (vendor != null) {
                    return vendor;
                }
            }
        }
        return UNKNOWN;
    }

    public String getName() {
        return this.name;
    }

    public boolean isExplainPlanSupported() {
        return this.explainPlanSupported;
    }

    public String getExplainPlanSql(String sql) throws SQLException {
        if (!isExplainPlanSupported()) {
            throw new SQLException("Unable to run explain plans for " + getName() + " databases");
        }
        return "EXPLAIN " + sql;
    }

    public String getType() {
        return this.type;
    }

    public String getHost(String url) {
        Matcher matcher = this.urlPattern.matcher(url);
        if (matcher.matches()) {
            String host = getHost(matcher);
            if (host != null) {
                return host;
            }
        }
        if (SIMPLE_DB_URL.matcher(url).matches()) {
            return "localhost";
        }
        return "UnknownOrLocalhost";
    }

    protected String getHost(Matcher matcher) {
        if (matcher.groupCount() >= 1) {
            return matcher.group(1);
        }
        return null;
    }

    public String getDatabase(String url) {
        Matcher matcher = this.urlPattern.matcher(url);
        if (matcher.matches()) {
            String db = getDatabase(matcher);
            if (db != null) {
                return db;
            }
        }
        matcher = SIMPLE_DB_URL.matcher(url);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return "Unknown";
    }

    protected String getDatabase(Matcher matcher) {
        if (matcher.groupCount() >= 2) {
            return matcher.group(2);
        }
        return null;
    }

    public MetricNameFormat getDatabaseMetricName(DatabaseMetaData metaData) {
        String databaseProductVersion = "Unknown";
        String databaseProductName = "Unknown";
        String host = "Unknown";
        String databaseName = "Unknown";

        if (metaData != null) {
            try {
                databaseProductVersion = metaData.getDatabaseProductVersion();
            } catch (Exception ex) {
            }
            try {
                databaseProductName = metaData.getDatabaseProductName();
            } catch (Exception ex) {
            }
            try {
                String url = metaData.getURL();
                host = getHost(url);
                databaseName = getDatabase(url);
            } catch (Exception ex) {
            }
        }
        return new SimpleMetricNameFormat(MessageFormat.format("RemoteService/Database/{0}/{1}/{2}/{3}/all",
                                                                      new Object[] {databaseProductName,
                                                                                           databaseProductVersion, host,
                                                                                           databaseName}));
    }

    public Collection<Collection<Object>> parseExplainPlanResultSet(int columnCount, ResultSet rs, RecordSql recordSql)
            throws SQLException {
        Collection explains = new LinkedList();
        while (rs.next()) {
            Collection values = new LinkedList();
            for (int i = 1; i <= columnCount; i++) {
                Object obj = rs.getObject(i);
                values.add(obj == null ? "" : obj.toString());
            }
            explains.add(values);
        }
        return explains;
    }

    public String getExplainPlanFormat() {
        return "text";
    }
}