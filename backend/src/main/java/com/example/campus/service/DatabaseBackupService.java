package com.example.campus.service;

import com.example.campus.dto.request.BackupRequest;
import com.example.campus.exception.BusinessException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatabaseBackupService {

    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter SQL_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;

    public DatabaseBackupService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public BackupFile createBackup(BackupRequest request) {
        boolean includeStructure = request == null || request.isIncludeStructure();
        boolean includeData = request == null || request.isIncludeData();
        if (!includeStructure && !includeData) {
            throw new BusinessException("请至少选择备份结构或备份数据");
        }

        try (Connection connection = dataSource.getConnection()) {
            String databaseName = getCurrentDatabase(connection);
            List<String> tables = getTables(connection, databaseName);
            String generatedAt = LocalDateTime.now().format(SQL_TIME_FORMAT);

            StringBuilder sql = new StringBuilder();
            sql.append("-- 校园综合服务平台数据库备份\n");
            sql.append("-- 数据库：").append(databaseName).append('\n');
            sql.append("-- 生成时间：").append(generatedAt).append("\n\n");
            sql.append("SET NAMES utf8mb4;\n");
            sql.append("SET FOREIGN_KEY_CHECKS=0;\n\n");

            if (includeStructure) {
                appendStructure(connection, sql, tables);
            }
            if (includeData) {
                appendData(connection, sql, tables);
            }

            sql.append("SET FOREIGN_KEY_CHECKS=1;\n");
            String filename = "campus_service_backup_" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".sql";
            return new BackupFile(filename, sql.toString().getBytes(StandardCharsets.UTF_8));
        } catch (SQLException ex) {
            throw new BusinessException("数据库备份失败：" + ex.getMessage());
        }
    }

    private String getCurrentDatabase(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT DATABASE()")) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        throw new BusinessException("无法获取当前数据库名称");
    }

    private List<String> getTables(Connection connection, String databaseName) throws SQLException {
        String sql = "SELECT TABLE_NAME FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = '" + escapeSql(databaseName) + "' AND TABLE_TYPE = 'BASE TABLE' " +
                "ORDER BY TABLE_NAME";
        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }
        return tables;
    }

    private void appendStructure(Connection connection, StringBuilder sql, List<String> tables) throws SQLException {
        sql.append("-- ----------------------------\n");
        sql.append("-- 表结构\n");
        sql.append("-- ----------------------------\n\n");

        for (String table : tables) {
            sql.append("DROP TABLE IF EXISTS ").append(quoteIdentifier(table)).append(";\n");
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SHOW CREATE TABLE " + quoteIdentifier(table))) {
                if (resultSet.next()) {
                    sql.append(resultSet.getString(2)).append(";\n\n");
                }
            }
        }
    }

    private void appendData(Connection connection, StringBuilder sql, List<String> tables) throws SQLException {
        sql.append("-- ----------------------------\n");
        sql.append("-- 表数据\n");
        sql.append("-- ----------------------------\n\n");

        for (String table : tables) {
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT * FROM " + quoteIdentifier(table))) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                boolean hasData = false;

                while (resultSet.next()) {
                    if (!hasData) {
                        sql.append("-- ").append(table).append("\n");
                        hasData = true;
                    }
                    sql.append("INSERT INTO ").append(quoteIdentifier(table)).append(" (");
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) {
                            sql.append(", ");
                        }
                        sql.append(quoteIdentifier(metaData.getColumnName(i)));
                    }
                    sql.append(") VALUES (");
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) {
                            sql.append(", ");
                        }
                        sql.append(formatValue(resultSet.getObject(i)));
                    }
                    sql.append(");\n");
                }
                if (hasData) {
                    sql.append('\n');
                }
            }
        }
    }

    private String formatValue(Object value) throws SQLException {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof BigDecimal) {
            return value.toString();
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "1" : "0";
        }
        if (value instanceof Timestamp timestamp) {
            return "'" + timestamp.toLocalDateTime().format(SQL_TIME_FORMAT) + "'";
        }
        if (value instanceof java.sql.Date date) {
            return "'" + date + "'";
        }
        if (value instanceof java.sql.Time time) {
            return "'" + time + "'";
        }
        if (value instanceof byte[] bytes) {
            return "0x" + toHex(bytes);
        }
        if (value instanceof Blob blob) {
            return "0x" + toHex(blob.getBytes(1, (int) blob.length()));
        }
        if (value instanceof Clob clob) {
            return "'" + escapeSql(clob.getSubString(1, (int) clob.length())) + "'";
        }
        return "'" + escapeSql(String.valueOf(value)) + "'";
    }

    private String quoteIdentifier(String value) {
        return "`" + value.replace("`", "``") + "`";
    }

    private String escapeSql(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\0", "\\0")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\u001A", "\\Z");
    }

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            hex.append(String.format("%02x", item));
        }
        return hex.toString();
    }

    public record BackupFile(String filename, byte[] content) {
    }
}
