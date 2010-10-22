// Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  MIT Licence

package com.joelhockey.cirrus;

import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StatementResultSet {
    private static final Log log = LogFactory.getLog(StatementResultSet.class);
    private Statement stmt;
    private ResultSet rs;
    public StatementResultSet(Statement stmt, ResultSet rs) {
        this.stmt = stmt;
        this.rs = rs;
    }
    public Statement getStatement() { return stmt; }
    public ResultSet getResultSet() { return rs; }
    public void close() {
        close(stmt, rs);
    }

    /**
     * Close statement and result set if not null. Ignore any exceptions
     * @param stmt statement or null
     * @param rs result set or null
     */
    public static void close(Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Throwable t) {
                log.error("Error closing jdbc resultset", t);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Throwable t) {
                log.error("Error closing jdbc statement", t);
            }
        }
    }
}
