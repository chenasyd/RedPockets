package com.redpockets.manager;

import com.redpockets.RedPocketsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物品红包预览管理器
 * 管理物品红包的预览功能，仅保存在内存中
 */
public class ItemRedPocketPreviewManager {

    private final RedPocketsPlugin plugin;
    // 预览数据: redPocketId -> 预览物品列表
    private final Map<String, List<ItemStack>> previewCache;
    // 过期检查任务ID
    private int cleanupTaskId = -1;

    public ItemRedPocketPreviewManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.previewCache = new ConcurrentHashMap<>();
    }

    /**
     * 初始化定时清理任务
     */
    public void initialize() {
        // 每5分钟清理一次过期的预览
        cleanupTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredPreviews,
            6000L, 6000L).getTaskId();
        plugin.getPluginLogger().info("物品红包预览管理器已启动");
    }

    /**
     * 保存红包预览数据
     * @param redPocketId 红包ID
     * @param items 物品列表
     * @param expiresAt 过期时间戳
     */
    public void savePreview(String redPocketId, List<ItemStack> items, long expiresAt) {
        if (redPocketId == null || items == null || items.isEmpty()) {
            return;
        }
        previewCache.put(redPocketId, new ArrayList<>(items));
        plugin.getPluginLogger().debug("保存红包预览: " + redPocketId + " 物品数: " + items.size());
    }

    /**
     * 获取红包预览数据
     * @param redPocketId 红包ID
     * @return 物品列表，不存在返回null
     */
    public List<ItemStack> getPreview(String redPocketId) {
        return previewCache.get(redPocketId);
    }

    /**
     * 打开红包预览GUI
     * @param player 玩家
     * @param redPocketId 红包ID
     */
    public void openPreviewGUI(Player player, String redPocketId) {
        List<ItemStack> items = getPreview(redPocketId);
        if (items == null || items.isEmpty()) {
            plugin.getMessageManager().sendError(player, "gui.redpocket.preview.not_found");
            return;
        }

        // 创建预览GUI
        int size = (int) Math.ceil(items.size() / 9.0) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        org.bukkit.inventory.Inventory inventory = Bukkit.createInventory(
            new PreviewInventoryHolder(redPocketId),
            size,
            plugin.getMessageManager().getMessage("gui.redpocket.preview.title")
        );

        // 添加物品
        for (int i = 0; i < items.size() && i < size; i++) {
            inventory.setItem(i, items.get(i));
        }

        player.openInventory(inventory);
    }

    /**
     * 移除预览数据
     * @param redPocketId 红包ID
     */
    public void removePreview(String redPocketId) {
        previewCache.remove(redPocketId);
        plugin.getPluginLogger().debug("移除红包预览: " + redPocketId);
    }

    /**
     * 清理过期预览
     */
    private void cleanupExpiredPreviews() {
        Iterator<Map.Entry<String, List<ItemStack>>> iterator = previewCache.entrySet().iterator();
        int cleanedCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<String, List<ItemStack>> entry = iterator.next();
            String redPocketId = entry.getKey();

            // 检查红包是否仍然有效
            var redPocketOpt = plugin.getRedPocketManager().getRedPocketFromCache(redPocketId);
            if (redPocketOpt.isEmpty() || !redPocketOpt.get().isValid()) {
                iterator.remove();
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            plugin.getPluginLogger().debug("清理过期预览: " + cleanedCount + " 个");
        }
    }

    /**
     * 关闭并清理
     */
    public void shutdown() {
        if (cleanupTaskId != -1) {
            Bukkit.getScheduler().cancelTask(cleanupTaskId);
            cleanupTaskId = -1;
        }
        previewCache.clear();
        plugin.getPluginLogger().info("物品红包预览管理器已关闭");
    }

    /**
     * 预览GUI持有者类
     */
    public static class PreviewInventoryHolder implements org.bukkit.inventory.InventoryHolder {
        private final String redPocketId;

        public PreviewInventoryHolder(String redPocketId) {
            this.redPocketId = redPocketId;
        }

        public String getRedPocketId() {
            return redPocketId;
        }

        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return null;
        }
    }
}
