package com.redpockets.manager;

import com.redpockets.RedPocketsPlugin;
import com.redpockets.database.DatabaseManager;
import org.bukkit.inventory.ItemStack;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

/**
 * 物品红包编辑持久化管理器
 * 负责保存和加载玩家编辑的物品数据
 */
public class ItemEditStorageManager {

    private final RedPocketsPlugin plugin;
    private final DatabaseManager databaseManager;

    public ItemEditStorageManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * 初始化数据库表
     */
    public void initialize() {
        String type = plugin.getConfigManager().getDatabaseType();

        try (Connection conn = databaseManager.getConnection()) {
            String sql;
            if (type.equalsIgnoreCase("mysql")) {
                sql = "CREATE TABLE IF NOT EXISTS item_edit_storage (" +
                        "uuid VARCHAR(36) PRIMARY KEY, " +
                        "items TEXT NOT NULL, " +
                        "redpocket_id VARCHAR(36), " +
                        "redpocket_expires_at BIGINT, " +
                        "updated_at BIGINT NOT NULL, " +
                        "INDEX idx_updated (updated_at)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            } else {
                sql = "CREATE TABLE IF NOT EXISTS item_edit_storage (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "items TEXT NOT NULL, " +
                        "redpocket_id TEXT, " +
                        "redpocket_expires_at INTEGER, " +
                        "updated_at INTEGER NOT NULL" +
                        ")";
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }

            plugin.getPluginLogger().info("物品编辑存储表创建完成！");
        } catch (SQLException e) {
            plugin.getPluginLogger().severe("创建物品编辑存储表失败！");
            e.printStackTrace();
        }
    }

    /**
     * 保存玩家编辑的物品
     */
    public void savePlayerItems(UUID playerUUID, ItemStack[] items) {
        savePlayerItems(playerUUID, items, null, 0);
    }

    /**
     * 保存玩家编辑的物品（关联红包信息）
     */
    public void savePlayerItems(UUID playerUUID, ItemStack[] items, String redPocketId, long redPocketExpiresAt) {
        String sql;
        String type = plugin.getConfigManager().getDatabaseType();

        if (type.equalsIgnoreCase("mysql")) {
            sql = "INSERT INTO item_edit_storage (uuid, items, redpocket_id, redpocket_expires_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE items = VALUES(items), redpocket_id = VALUES(redpocket_id), " +
                    "redpocket_expires_at = VALUES(redpocket_expires_at), updated_at = VALUES(updated_at)";
        } else {
            sql = "INSERT OR REPLACE INTO item_edit_storage (uuid, items, redpocket_id, redpocket_expires_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?)";
        }

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String itemsData = serializeItems(items);
            long updatedAt = System.currentTimeMillis();

            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, itemsData);
            stmt.setString(3, redPocketId);
            stmt.setLong(4, redPocketExpiresAt);
            stmt.setLong(5, updatedAt);

            stmt.executeUpdate();

        } catch (SQLException | IOException e) {
            plugin.getPluginLogger().severe("保存玩家编辑物品失败！UUID: " + playerUUID);
            e.printStackTrace();
        }
    }

    /**
     * 加载玩家编辑的物品
     */
    public ItemStack[] loadPlayerItems(UUID playerUUID) {
        String sql = "SELECT items, redpocket_id, redpocket_expires_at FROM item_edit_storage WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String itemsData = rs.getString("items");
                    return deserializeItems(itemsData);
                }
            }

        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getPluginLogger().severe("加载玩家编辑物品失败！UUID: " + playerUUID);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取玩家关联的红包ID
     */
    public String getRedPocketId(UUID playerUUID) {
        String sql = "SELECT redpocket_id FROM item_edit_storage WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("redpocket_id");
                }
            }

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("获取红包ID失败！UUID: " + playerUUID);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取红包过期时间
     */
    public long getRedPocketExpiresAt(UUID playerUUID) {
        String sql = "SELECT redpocket_expires_at FROM item_edit_storage WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("redpocket_expires_at");
                }
            }

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("获取红包过期时间失败！UUID: " + playerUUID);
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * 检查物品是否被锁定（已发送红包且未过期）
     */
    public boolean isItemsLocked(UUID playerUUID) {
        String redPocketId = getRedPocketId(playerUUID);
        if (redPocketId == null) return false;

        long expiresAt = getRedPocketExpiresAt(playerUUID);
        if (expiresAt <= 0) return false;

        return System.currentTimeMillis() < expiresAt;
    }

    /**
     * 删除玩家编辑的物品
     */
    public void deletePlayerItems(UUID playerUUID) {
        String sql = "DELETE FROM item_edit_storage WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("删除玩家编辑物品失败！UUID: " + playerUUID);
            e.printStackTrace();
        }
    }

    /**
     * 清除红包关联（红包被完全领完或删除时调用）
     */
    public void clearRedPocketAssociation(UUID playerUUID) {
        String sql = "UPDATE item_edit_storage SET redpocket_id = NULL, redpocket_expires_at = 0 WHERE uuid = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("清除红包关联失败！UUID: " + playerUUID);
            e.printStackTrace();
        }
    }

    /**
     * 序列化物品数组为Base64字符串
     */
    private String serializeItems(ItemStack[] items) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             org.bukkit.util.io.BukkitObjectOutputStream objectStream = new org.bukkit.util.io.BukkitObjectOutputStream(byteStream)) {

            objectStream.writeObject(items);
            objectStream.flush();
            return Base64.getEncoder().encodeToString(byteStream.toByteArray());
        }
    }

    /**
     * 从Base64字符串反序列化物品数组
     */
    private ItemStack[] deserializeItems(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
             org.bukkit.util.io.BukkitObjectInputStream objectStream = new org.bukkit.util.io.BukkitObjectInputStream(byteStream)) {

            return (ItemStack[]) objectStream.readObject();
        }
    }
}
