// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joelhockey.codec.Hex;
import com.joelhockey.codec.JSON;

/**
 * Wrapper for jdbc to make sql easier.
 * This class is NOT thread-safe.
 * @author Joel Hockey
 */
public class DB {
    private static final Log log = LogFactory.getLog(DB.class);
    private static final Object[] EMPTY = null;
    private static final Map<Integer, String> TYPES = new HashMap<Integer, String>();

    static {
        try {
            for (Field f : Types.class.getDeclaredFields()) {
                // only put 'public static final int' in map
                if (f.getType() == int.class && Modifier.isPublic(f.getModifiers())
                        && Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
                    TYPES.put(f.getInt(null), f.getName());
                }
            }
        } catch (Exception e) {} // ignore
    }

    private DataSource dataSource;
    private Connection dbconn;
    private boolean autoCommit;

    /**
     * Construct DB with data source and auto-commit=true.
     * This class is NOT thread-safe.
     * @param dataSource data source
     */
    public DB(DataSource dataSource) {
        this(dataSource, true);
    }

    /**
     * Construct DB with data source and specified auto-commit.
     * This class is NOT thread-safe.
     * @param dataSource data source
     * @param autoCommit true if auto-commit
     */
    public DB(DataSource dataSource, boolean autoCommit) {
        this.dataSource = dataSource;
        this.autoCommit = autoCommit;
    }

    /**
     * Open database connection.
     * @throws SQLException if error getting db connection
     */
    public void open() throws SQLException {
        if (dbconn == null) {
            dbconn = dataSource.getConnection();
            dbconn.setAutoCommit(autoCommit);
        } else {
            log.warn("ignoring DB.open, dbconn already open");
        }
    }

    /**
     * Close DB connection.
     */
    public void close() {
        try {
            if (dbconn != null) {
                dbconn.close();
                dbconn = null;
            }
        } catch (Exception e) {
            log.error("Error closing dbconn", e);
        }
    }

    /** commit - ignores any errors */
    public void commit() {
        boolean ok = false;
        long start = System.currentTimeMillis();
        try {
            dbconn.commit();
            ok = true;
        } catch (Exception e) {
            log.error("Commitment issues", e);
        } finally {
            long timeTaken = System.currentTimeMillis() - start;
            log.debug(format("sql: commit : %s : 0 : %05d",
                ok ? "ok" : "error", timeTaken));
        }
    }

    /** rollback - ignores any errors */
    public void rollback() {
        boolean ok = false;
        long start = System.currentTimeMillis();
        try {
            dbconn.rollback();
            ok = true;
        } catch (Exception e) {
            log.error("Error rolling back", e);
        } finally {
            long timeTaken = System.currentTimeMillis() - start;
            log.debug(format("sql: rollback : %s : 0 : %05d",
                ok ? "ok" : "error", timeTaken));
        }
    }

    /** @return db connection */
    public Connection getConnection() {
        return dbconn;
    }

    /**
     * execute.
     * @param sql sql statement(s) to execute
     * @throws SQLException
     */
    public void execute(String sql) throws SQLException {
        executeAndClose(sql, "execute", EMPTY);
    }

    /**
     * insert.
     * @param sql sql insert statement
     * @return number of records inserted
     * @throws SQLException if sql error
     */
    public int insert(String sql) throws SQLException {
        return insert(sql, EMPTY);
    }

    /**
     * insert.
     * @param sql sql insert statement with '?' for params
     * @param params params
     * @return number of records inserted
     * @throws SQLException if sql error
     */
    public int insert(String sql, Object... params) throws SQLException {
        return executeAndClose(sql, "insert", params);
    }

    /**
     * insert record.  Column names convert from camelCase to under_score.
     * @param table name of table
     * @param record Map that represents record
     * @return number of records inserted.
     * @throws SQLException if sql error
     * @throws CodecException if error parsing JSON
     * @throws ClassCastException if JSON not array of objects
     */
    public int insert(String table, Map<String, Object> record) throws SQLException {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>(1);
        records.add(record);
        return insertAll(table, records);
    }

    /**
     * insert json object or array of objects
     * Column names convert from camelCase to under_score.
     * @param table name of table
     * @param json json formatted object or array of objects
     * @return number of records inserted.
     * @throws SQLException if sql error
     * @throws CodecException if error parsing JSON
     * @throws ClassCastException if JSON not array of objects
     */
    public int insertJson(String table, String json) throws SQLException {
        Object js = JSON.parse(json);
        if (js instanceof Map) {
            return insert(table, (Map) js);
        }
        return insertAll(table, (List) JSON.parse(json));
    }

    /**
     * insert list of maps.  Column names convert from camelCase
     * to under_score.
     * @param table name of table
     * @param records List of Maps that represent records
     * @return number of records inserted.
     * @throws SQLException if sql error
     * @throws CodecException if error parsing JSON
     * @throws ClassCastException if JSON not array of objects
     */
    public int insertAll(String table, List<Map<String, Object>> records)
            throws SQLException {

        for (int i = 0; i < records.size(); i++) {
            Map<String, Object> record = records.get(i);
            StringBuilder insert = new StringBuilder("insert into " + table + "(");
            StringBuilder qmarks = new StringBuilder(" values (");

            String[] cols = record.keySet().toArray(new String[0]);
            Object[] params = new Object[cols.length];
            boolean comma = false;
            for (int j = 0; j < cols.length; j++) {
                String camelCase = cols[j];
                params[j] = record.get(camelCase);
                StringBuilder colname = new StringBuilder();
                boolean lastLower = false;
                for (int k = 0; k < camelCase.length(); k++) {
                    char c = camelCase.charAt(k);
                    if (Character.isLowerCase(c)) {
                        lastLower = true;
                    } else {
                        // if last char lower and this one upper, then add underscore
                        if (lastLower) {
                            colname.append('_');
                        }
                        lastLower = false;
                        c = Character.toLowerCase(c);
                    }
                    colname.append(c);
                }
                if (comma) {
                    insert.append(',');
                    qmarks.append(',');
                }
                comma = true;
                insert.append(colname);
                qmarks.append('?');
            }
            String sql = insert.append(')').append(qmarks).append(')').toString();
            insert(sql, params);
        }
        return records.size();
    }

    /**
     * update.
     * @param sql sql update statement
     * @return number of records updated
     * @throws SQLException if sql error
     */
    public int update(String sql) throws SQLException {
        return update(sql, EMPTY);
    }

    /**
     * update.
     * @param sql sql update statement with '?' for params
     * @param params params
     * @return number of records updated
     * @throws SQLException if sql error
     */
    public int update(String sql, Object... params) throws SQLException {
        return executeAndClose(sql, "update", params);
    }

    /**
     * execute.  Caller must close statement.
     * @param sql sql execute, insert, update, or delete statement with '?' for params
     * @param sqlcmd 'execute', 'insert', 'update', 'delete', etc used for logging
     * @param params params
     * @return prepared statement
     * @throws SQLException if sql error
     */
    public PreparedStatement execute(String sql, String sqlcmd, Object... params) throws SQLException {
        long start = System.currentTimeMillis();
        boolean ok = false;
        int count = -1;
        PreparedStatement stmt = null;

        try {
            stmt = dbconn.prepareStatement(sql);
            setParams(stmt, params);
            if (!stmt.execute()) { // returns false if result is count
                count = stmt.getUpdateCount();
            }
            ok = true;
            return stmt;
        } finally {
            long timeTaken = System.currentTimeMillis() - start;
            log.debug(format("sql: %s : %s : %d : %05d : %s : %s",
                    sqlcmd, ok ? "ok" : "error", count, timeTaken, sql,
                    JSON.stringify(params)));
        }
    }

    /**
     * execute.
     * @param sql sql execute, insert, update, or delete statement with '?' for params
     * @param sqlcmd 'execute', 'insert', 'update', 'delete', etc used for logging
     * @param params params
     * @return number of records inserted or updated
     * @throws SQLException if sql error
     */
    private int executeAndClose(String sql, String sqlcmd, Object... params) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = execute(sql, sqlcmd, params);
            return stmt.getUpdateCount();
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                    log.error("Error closing stmt", e);
                }
            }
        }
    }

    /**
     * delete.
     * @param sql sql delete statement
     * @return number of records deleted
     * @throws SQLException if sql error
     */
    public int delete(String sql) throws SQLException {
        return delete(sql, EMPTY);
    }

    /**
     * delete.
     * @param sql sql delete statement with '?' for params
     * @param params params
     * @return number of records deleted
     * @throws SQLException if sql error
     */
    public int delete(String sql, Object... params) throws SQLException {
        return executeAndClose(sql, "delete", params);
    }

    /**
     * delete without using javascript 'delete' keyword.
     * @param sql sql delete statement with '?' for params
     * @return number of records deleted
     * @throws SQLException if sql error
     */
    public int del(String sql) throws SQLException {
        return delete(sql, EMPTY);
    }

    /**
     * delete without using javascript 'delete' keyword.
     * @param sql sql delete statement with '?' for params
     * @param params params
     * @return number of records deleted
     * @throws SQLException if sql error
     */
    public int del(String sql, Object... params) throws SQLException {
        return delete(sql, params);
    }

    /**
     * select. Caller MUST close statement.
     * @param sql sql select statement
     * @return PreparedStatement and ResultSet
     * @throws SQLException if sql error
     */
    public StatementResultSet select(String sql) throws SQLException {
        return select(sql, EMPTY);
    }

    /**
     * select. Caller MUST close statement.
     * @param sql sql select statement with '?' for params
     * @param params params
     * @return [PreparedStatement, ResultSet]
     * @throws SQLException if sql error
     */
    public StatementResultSet select(String sql, Object... params) throws SQLException {
        long start = System.currentTimeMillis();
        boolean ok = false;
        try {
            PreparedStatement stmt = dbconn.prepareStatement(sql);
            setParams(stmt, params);
            ResultSet rs = stmt.executeQuery();
            ok = true;
            return new StatementResultSet(stmt, rs);
        } finally {
            long timeTaken = System.currentTimeMillis() - start;
            log.debug(format("sql: select : %s : %05d : %s : %s",
                    ok ? "ok" : "error", timeTaken, sql, JSON.stringify(params)));
        }
    }

    /**
     * Return all rows
     * @param sql sql select statement with '?' for params
     * @param params params
     * @return List&lt;Map&lt;String, Object>> list of rows
     * @throws SQLException if sql error
     */
    public List<Map<String, Object>> selectAll(String sql) throws SQLException {
        return selectAll(sql, EMPTY);
    }

    /**
     * Return all rows
     * @param sql sql select statement with '?' for params
     * @param params params
     * @return List&lt;Map&lt;String, Object>> list of rows
     * @throws SQLException if sql error
     */
    public List<Map<String, Object>> selectAll(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        boolean ok = false;
        StatementResultSet stmtRs = select(sql, params);
        long start = System.currentTimeMillis();
        try {
            ResultSet rs = stmtRs.getResultSet();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<String, Object>();
                result.add(row);
                // sql stuff is 1-based!
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value = null;
                    switch (meta.getColumnType(i)) {
                    case Types.DATE:
                        value = rs.getDate(i);
                        break;
                    case Types.INTEGER:
                        value = rs.getInt(i);
                        break;
                    case Types.VARCHAR:
                        value = rs.getString(i);
                        break;
                    case Types.TIMESTAMP:
                        value = new java.util.Date(rs.getTimestamp(i).getTime());
                        break;
                    default:
                        int type = meta.getColumnType(i);
                        throw new SQLException("unrecognised type: " + type
                                + "(" + TYPES.get(type) + ")");
                    }
                    StringBuilder label = new StringBuilder();
                    String[] parts = meta.getColumnName(i).split("_");
                    for (int j = 0; j < parts.length; j++) {
                        String part = parts[j].toLowerCase();
                        if (j > 0) {
                            part = part.substring(0, 1).toUpperCase() + part.substring(1);
                        }
                        label.append(part);
                    }
                    row.put(label.toString(), value);
                }
            }
            ok = true;
        } finally {
            stmtRs.close();
            long timeTaken = System.currentTimeMillis() - start;
            log.debug(format("sql: selectAll : %s : %05d : %d", ok ? "ok" : "error", timeTaken, result.size()));
        }
        return result;
    }

    /**
     * select int.
     * @param sql sql statement that selects a single int value
     * @return result
     * @throws SQLException if sql error
     */
    public int selectInt(String sql) throws SQLException {
        return selectInt(sql, EMPTY);
    }

    /**
     * select int.
     * @param sql sql statement with '?' for params that selects a single int value
     * @param params params
     * @return result
     * @throws SQLException if sql error
     */
    public int selectInt(String sql, Object... params) throws SQLException {
        StatementResultSet stmtRs = select(sql, params);
        try {
            if (!stmtRs.getResultSet().next()) {
                throw new SQLException(format("No records found for sql: %s, %s",
                        sql, JSON.stringify(params)));
            }
            int result = stmtRs.getResultSet().getInt(1);
            try {
                if (stmtRs.getResultSet().next()) {
                    log.warn(format("More than 1 object returned for sql: %s , %s",
                            sql, JSON.stringify(params)));
                }
            } catch (Throwable t) {
            } // ignore
            return result;
        } finally {
            stmtRs.getStatement().close();
        }
    }

    /**
     * select string.
     * @param sql sql select statement that selects a single string
     * @return result
     * @throws SQLException if sql error
     */
    public String selectStr(String sql) throws SQLException {
        return selectStr(sql, EMPTY);
    }

    /**
     * select string.
     * @param dbconn db connection
     * @param sql sql statement with '?' for params that selects a single string value
     * @param params params
     * @return result
     * @throws SQLException if sql error
     */
    public String selectStr(String sql, Object... params) throws SQLException {
        StatementResultSet stmtRs = select(sql, params);
        try {
            if (!stmtRs.getResultSet().next()) {
                throw new SQLException(format("No records found for sql: %s, %s",
                        sql, JSON.stringify(params)));
            }
            String result = stmtRs.getResultSet().getString(1);
            try {
                if (stmtRs.getResultSet().next()) {
                    log.warn(format("More than 1 object returned for sql: %s , %s",
                            sql, JSON.stringify(params)));
                }
            } catch (Throwable t) {} // ignore
            return result;
        } finally {
            stmtRs.getStatement().close();
        }
    }

    /**
     * Set params on PreparedStatement. Uses reflection to determine param type and call appropriate setter on
     * statement.
     * @param stmt statement
     * @param params params
     * @throws SQLException if sql error
     */
    public void setParams(PreparedStatement stmt, Object... params) throws SQLException {
        if (params == null || params.length == 0) {
            return;
        }

        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p == null) {
                // assume VARCHAR for null
                stmt.setNull(i + 1, Types.VARCHAR);
            } else if (p instanceof String) {
                stmt.setString(i + 1, (String) p);
            } else if (p instanceof Integer) {
                stmt.setInt(i + 1, (Integer) p);
            } else if (p instanceof Double) {
                stmt.setDouble(i + 1, (Double) p);
            } else if (p instanceof Date) {
                stmt.setTimestamp(i + 1, new java.sql.Timestamp(((Date) p).getTime()));
            } else if (p instanceof Boolean) {
                stmt.setBoolean(i + 1, (Boolean) p);
            } else if (p instanceof byte[]) {
                stmt.setString(i + 1, Hex.b2s((byte[]) p));
            } else if (p instanceof Throwable) {
                StringWriter sw = new StringWriter();
                ((Throwable) p).printStackTrace(new PrintWriter(sw));
                stmt.setString(i + 1, sw.toString());
            } else {
                throw new SQLException("unknown type in sql param class: " + p.getClass() + " : p: " + p);
            }
        }
    }

    /**
     * Helper to get clob from result set.
     * @param rs result set
     * @param col column number
     * @return clob retrieved as a string
     * @throws IOException if io error
     * @throws SQLException if sql error
     */
    public String getClob(ResultSet rs, int col) throws IOException, SQLException {
        char[] cbuf = new char[4096];
        return getClob(rs, col, cbuf);
    }

    /**
     * Helper to get clob from result set with user provided temp storage buffer.
     * @param rs result set
     * @param col column number
     * @param cbuf user provided temp storage buffer
     * @return clob retrieved as a string
     * @throws IOException if io error
     * @throws SQLException if sql error
     */
    public String getClob(ResultSet rs, int col, char[] cbuf) throws IOException, SQLException {
        Reader cs = rs.getCharacterStream(col);
        if (cs == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        while (true) {
            int l = cs.read(cbuf);
            if (l == -1) {
                return sb.toString();
            }
            sb.append(cbuf, 0, l);
        }
    }

    /**
     * Migrate database to specified version using
     * files from '/db/migrate/'
     * @param versionRequired optional version to migrate to if null,
     * will use all available files in '/db/migrate/'
     * @return previous version
     * @throws SQLException if sql error
     * @throws IOException if error reading files
     */
    public int migrate(Integer versionRequired) throws SQLException, IOException {
        // create timer, and DB object
        Timer timer = new Timer();
        timer.start();
        boolean needToClose = false;
        if (dbconn == null) {
            needToClose = true;
            open();
        }
        try {
            int versionCurrent;
            try {
                // get current version from 'db_version' table
                versionCurrent = selectInt("select max(version) from db_version");
            } catch (Exception e) {
                // error reading from 'db_version' table, try init script
                log.warn("Error getting dbversion, will try and load init script: " + e);
                String sql = Resource.readFile("/db/000_init.sql");
                execute(sql);
                insert("insert into db_version (version, filename, script) values (0, '000_init.sql', ?)", sql);
                timer.mark("db init");
                versionCurrent = selectInt("select max(version) from db_version");
            }

            // check if up to date
            String msg = "db at version: " + versionCurrent
                + ", app requires version: " + versionRequired;
            if (versionRequired != null && versionCurrent == versionRequired.intValue()) {
                log.info("db version ok.  " + msg);
                return versionCurrent;
            } else if (versionRequired != null
                    && versionCurrent > versionRequired.intValue()) {
                // very strange
                throw new SQLException(msg);
            }

            // move from versionCurrent to versionRequired
            log.info("db migration: " + msg);

            // look in dir '/db/migrate' to find required files
            Set<String> files = Resource.getResourcePaths("/db/migrate/");
            if (files == null || files.size() == 0) {
                throw new SQLException("No files found in /db/migrate/");
            }
            log.info("files in '/db/migrate/': " + files);
            Map<Integer, String> fileMap = new HashMap<Integer, String>();
            int versionMax = 0;
            Pattern p = Pattern.compile("^\\/db\\/migrate\\/(\\d{3})_.*\\.sql$");
            for (String file : files) {
                // check for filename format <nnn>_<description>.sql
                Matcher m = p.matcher(file);
                if (m.matches()) {
                    int filenum = Integer.parseInt(m.group(1));
                    versionMax = Math.max(versionMax, filenum);
                    if (filenum > versionCurrent) {
                        // check for duplicates
                        String existing = fileMap.get(file);
                        if (existing != null) {
                            throw new java.sql.SQLException(
                                    "Found duplicate file for migration: "
                                    + existing + ", " + file);
                        }
                        fileMap.put(filenum, file);
                    }
                }
            }

            // if versionRequired not provided, set to max version found
            if (versionRequired == null) {
                log.warn("db migrate target version not provided, "
                        + "using max value found: " + versionMax);
                versionRequired = versionMax;
            }

            // ensure all files exist
            for (int i = versionCurrent + 1; i <= versionRequired; i++) {
                if (!fileMap.containsKey(i)) {
                    throw new SQLException("Migrating from: " + versionCurrent
                        + " to: " + versionRequired + ", missing file: "
                        + i + ", got files: " + fileMap);
                }
            }

            timer.mark("check version");
            // run scripts
            for (int i = versionCurrent + 1; i <= versionRequired; i++) {
                String script = fileMap.get(i);
                log.info("db migration running script: " + script);
                String sql = Resource.readFile(script);
                insert("insert into db_version (version, filename, script) values (?, ?, ?)", i, script, sql);
                execute(sql);
                timer.mark(script);
            }

            // commit now
            commit();
            return versionCurrent;
        } catch (SQLException sqle) {
            // rollback on error
            rollback();
            throw sqle;
        } catch (IOException ioe) {
            // rollback on error
            rollback();
            throw ioe;
        } finally {
            // only close if we opened this connection
            if (needToClose) {
                close();
            }
            timer.end("DB migration");
        }
    };
}