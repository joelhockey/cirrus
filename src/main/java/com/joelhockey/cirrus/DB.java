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
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joelhockey.codec.Hex;
import com.joelhockey.codec.JSON;

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

    private Connection dbconn;

    /**
     * Construct DB with data source.
     * @param scope js scope
     * @param dataSource data source
     * @throws SQLException if error getting connection from data source
     */
    public DB(DataSource dataSource) throws SQLException {
        this.dbconn = dataSource.getConnection();
    }

    /**
     * Close DB connection.
     */
    public void close() {
        try {
            if (dbconn != null) {
                dbconn.close();
            }
        } catch (Exception e) {
            log.error("Error closing dbconn", e);
        }
    }

    /**
     * execute.
     * @param sql sql statement(s) to execute
     * @throws SQLException
     */
    public void execute(String sql) throws SQLException {
        update(sql, "execute", EMPTY);
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
        return update(sql, "insert", params);
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
        return update(sql, "update", params);
    }

    /**
     * update.
     * @param sql sql insert or update statement with '?' for params
     * @param sqlcmd 'insert' or 'update' used for logging
     * @param params params
     * @return number of records inserted or updated
     * @throws SQLException if sql error
     */
    private int update(String sql, String sqlcmd, Object... params) throws SQLException {
        long start = System.currentTimeMillis();
        boolean ok = false;
        int count = -1;
        PreparedStatement stmt = null;

        try {
            stmt = dbconn.prepareStatement(sql);
            setParams(stmt, params);

            count = stmt.executeUpdate();
            ok = true;
            return count;
        } finally {
            long timeTaken = System.currentTimeMillis() - start;
            log.debug(format("sql: %s : %s : %d : %05d : %s : %s",
                    sqlcmd, ok ? "ok" : "error", count, timeTaken, sql,
                    JSON.stringify(params)));
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
        return update(sql, "delete", params);
    }

    /**
     * delete without using javascript 'delete' keyword.
     * @param sql sql delete statement with '?' for params
     * @return number of records deleted
     * @throws SQLException if sql error
     */
    public int dl33t(String sql) throws SQLException {
        return delete(sql, EMPTY);
    }

    /**
     * delete without using javascript 'delete' keyword.
     * @param sql sql delete statement with '?' for params
     * @param params params
     * @return number of records deleted
     * @throws SQLException if sql error
     */
    public int dl33t(String sql, Object... params) throws SQLException {
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
            } catch (Throwable t) {
            } // ignore
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
}