package com.redpockets.database;

import com.redpockets.RedPocketsPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * 数据库管理器
 * 管理数据库连接和数据操作
 */
public class DatabaseManager {

    private final RedPocketsPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化数据库连接
     */
    public void initialize() throws SQLException {
        String type = plugin.getConfigManager().getDatabaseType();

        HikariConfig config = new HikariConfig();

        if (type.equalsIgnoreCase("mysql")) {
            configureMySQL(config);
        } else {
            configureSQLite(config);
        }

        dataSource = new HikariDataSource(config);

        // 创建表
        createTables();

        plugin.getPluginLogger().info("数据库初始化完成 (类型: " + type + ")");
    }

    /**
     * 配置MySQL连接
     */
    private void configureMySQL(HikariConfig config) {
        String host = plugin.getConfigManager().getMySQLHost();
        int port = plugin.getConfigManager().getMySQLPort();
        String database = plugin.getConfigManager().getMySQLDatabase();
        String username = plugin.getConfigManager().getMySQLUsername();
        String password = plugin.getConfigManager().getMySQLPassword();

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&characterEncoding=UTF-8&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(plugin.getConfigManager().getConnectionPoolSize());
        config.setMinimumIdle(plugin.getConfigManager().getMinimumIdle());
        config.setConnectionTimeout(plugin.getConfigManager().getConnectionTimeout());
        config.setPoolName("RedPockets-Pool");
    }

    /**
     * 配置SQLite连接
     */
    private void configureSQLite(HikariConfig config) {
        String file = plugin.getConfigManager().getSQLiteFile();
        String path = new java.io.File(plugin.getDataFolder(), file).getAbsolutePath();

        config.setJdbcUrl("jdbc:sqlite:" + path);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite不支持多连接
        config.setMinimumIdle(1);
        config.setConnectionTimeout(plugin.getConfigManager().getConnectionTimeout());
        config.setPoolName("RedPockets-SQLite-Pool");
    }

    /**
     * 创建数据库表
     */
    private void createTables() {
        String type = plugin.getConfigManager().getDatabaseType();

        try (Connection conn = getConnection()) {
            // 创建红包表
            createRedPocketTable(conn, type);

            // 创建红包记录表
            createRedPocketRecordTable(conn, type);

            plugin.getPluginLogger().info("数据库表创建完成！");
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("创建数据库表失败！");
            e.printStackTrace();
        }
    }

    /**
     * 创建红包表
     */
    private void createRedPocketTable(Connection conn, String type) throws SQLException {
        String sql;
        if (type.equalsIgnoreCase("mysql")) {
            sql = "CREATE TABLE IF NOT EXISTS redpockets (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "sender VARCHAR(36) NOT NULL, " +
                    "type VARCHAR(20) NOT NULL, " +
                    "total_amount DOUBLE NOT NULL, " +
                    "count INT NOT NULL, " +
                    "note VARCHAR(50), " +
                    "created_at BIGINT NOT NULL, " +
                    "expires_at BIGINT, " +
                    "is_claimed BOOLEAN DEFAULT FALSE, " +
                    "INDEX idx_sender (sender), " +
                    "INDEX idx_created (created_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS redpockets (" +
                    "id TEXT PRIMARY KEY, " +
                    "sender TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "total_amount REAL NOT NULL, " +
                    "count INTEGER NOT NULL, " +
                    "note TEXT, " +
                    "created_at INTEGER NOT NULL, " +
                    "expires_at INTEGER, " +
                    "is_claimed INTEGER DEFAULT 0" +
                    ")";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    /**
     * 创建红包记录表
     */
    private void createRedPocketRecordTable(Connection conn, String type) throws SQLException {
        String sql;
        if (type.equalsIgnoreCase("mysql")) {
            sql = "CREATE TABLE IF NOT EXISTS redpocket_records (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "redpocket_id VARCHAR(36) NOT NULL, " +
                    "claimer VARCHAR(36) NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "claimed_at BIGINT NOT NULL, " +
                    "FOREIGN KEY (redpocket_id) REFERENCES redpockets(id) ON DELETE CASCADE, " +
                    "INDEX idx_redpocket (redpocket_id), " +
                    "INDEX idx_claimer (claimer)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS redpocket_records (" +
                    "id TEXT PRIMARY KEY, " +
                    "redpocket_id TEXT NOT NULL, " +
                    "claimer TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "claimed_at INTEGER NOT NULL, " +
                    "FOREIGN KEY (redpocket_id) REFERENCES redpockets(id) ON DELETE CASCADE" +
                    ")";
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    /**
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("数据库连接池未初始化或已关闭");
        }
        return dataSource.getConnection();
    }

    /**
     * 关闭数据库连接
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getPluginLogger().info("数据库连接池已关闭。");
        }
    }
}
