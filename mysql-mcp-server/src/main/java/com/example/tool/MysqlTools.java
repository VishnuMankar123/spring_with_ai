package com.example.tool;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.regex.Pattern;

@Service
public class MysqlTools {

    private static final Logger log = LoggerFactory.getLogger(MysqlTools.class);

    private final DataSource dataSource;

    private static final Pattern TABLE_NAME = Pattern.compile("[A-Za-z0-9_]+");

    public MysqlTools(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // TOOL 1: List all tables

    @McpTool(description = "List all tables available in the configured MySQL database")
    public String listTables() {

        log.info("MCP TOOL CALLED: listTables");

        StringBuilder result = new StringBuilder();
        String sql = "SELECT TABLE_NAME FROM information_schema.tables "
                + "WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {
                result.append(rs.getString(1)).append("\n");
            }

            return result.length() == 0
                    ? "No tables found in database '" + connection.getCatalog() + "'"
                    : result.toString();

        } catch (Exception e) {
            log.error("Error listing tables", e);
            return "Database error: " + e.getMessage();
        }
    }

    // TOOL 2: Describe table

    @McpTool(description = "Get the columns, data types and structure of a MySQL table")
    public String describeTable(@McpToolParam(description = "Name of the database table", required = true) String tableName) {
        log.info("MCP TOOL CALLED: describeTable: {}", tableName);

        if (tableName == null || !TABLE_NAME.matcher(tableName).matches()) {return "Invalid table name";}

        StringBuilder result = new StringBuilder();

        String sql = "DESCRIBE `" + tableName + "`";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            while (rs.next()) {

                result.append("Field: ")
                        .append(rs.getString("Field"))
                        .append(", Type: ")
                        .append(rs.getString("Type"))
                        .append(", Null: ")
                        .append(rs.getString("Null"))
                        .append(", Key: ")
                        .append(rs.getString("Key"))
                        .append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            log.error("Error describing table", e);
            return "Database error: " + e.getMessage();
        }
    }

    // TOOL 3: Execute read-only SQL query

    @McpTool(description = "Execute a read-only SELECT, SHOW, DESCRIBE or EXPLAIN SQL query. " + "Use this tool to retrieve records and analyze database data.")
    public String executeReadOnlyQuery(@McpToolParam(description = "Read-only SQL query to execute", required = true) String sql) {

        log.info("MCP TOOL CALLED: executeReadOnlyQuery: {}", sql);

        String validatedSql;

        try {
            validatedSql = validateSql(sql);
        } catch (IllegalArgumentException e) {
            return "Query rejected: " + e.getMessage();
        }

        try (Connection connection = dataSource.getConnection()) {

            // Request a read-only connection
            connection.setReadOnly(true);

            try (Statement statement = connection.createStatement()) {

                statement.setMaxRows(100);
                statement.setQueryTimeout(15);

                try (ResultSet rs =
                             statement.executeQuery(validatedSql)) {

                    return formatResult(rs);
                }
            }

        } catch (Exception e) {

            log.error("SQL execution failed", e);

            return "SQL execution failed: " + e.getMessage();
        }
    }

    private String validateSql(String sql) {

        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL cannot be empty");
        }

        String query = sql.trim();

        // Remove one optional trailing semicolon
        if (query.endsWith(";")) {
            query = query.substring(0, query.length() - 1).trim();
        }

        // Do not allow multiple statements
        if (query.contains(";")) {
            throw new IllegalArgumentException(
                    "Multiple SQL statements are not allowed");
        }

        String upper = query.toUpperCase();

        // Only read operations
        if (!upper.matches(
                "(?s)^(SELECT|SHOW|DESCRIBE|DESC|EXPLAIN)\\b.*")) {

            throw new IllegalArgumentException(
                    "Only SELECT, SHOW, DESCRIBE and EXPLAIN are allowed");
        }

        // Block potentially dangerous SELECT patterns
        String[] blocked = {
                "INTO OUTFILE",
                "INTO DUMPFILE",
                "LOAD_FILE(",
                "SLEEP(",
                "BENCHMARK(",
                "FOR UPDATE",
                "LOCK IN SHARE MODE"
        };

        for (String keyword : blocked) {
            if (upper.contains(keyword)) {
                throw new IllegalArgumentException(
                        "Unsafe SQL operation detected: " + keyword);
            }
        }

        return query;
    }

    private String formatResult(ResultSet rs) throws Exception {

        ResultSetMetaData metaData = rs.getMetaData();

        int columnCount = metaData.getColumnCount();

        StringBuilder result = new StringBuilder();

        // Column names
        for (int i = 1; i <= columnCount; i++) {

            result.append(metaData.getColumnLabel(i));

            if (i < columnCount) {
                result.append(" | ");
            }
        }

        result.append("\n");

        result.append("----------------------------------\n");

        // Rows
        int rowCount = 0;

        while (rs.next()) {

            for (int i = 1; i <= columnCount; i++) {

                result.append(rs.getString(i));

                if (i < columnCount) {
                    result.append(" | ");
                }
            }

            result.append("\n");
            rowCount++;
        }

        result.append("\nTotal rows: ").append(rowCount);

        return result.toString();
    }
}
