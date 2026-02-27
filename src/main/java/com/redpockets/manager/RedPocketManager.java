package com.redpockets.manager;

import com.redpockets.RedPocketsPlugin;
import com.redpockets.model.RedPocket;
import com.redpockets.model.RedPocketRecord;
import net.md_5.bungee.api.chat.TextComponent;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 红包管理器
 * 管理红包的创建、抢取和查询
 */
public class RedPocketManager {

    private final RedPocketsPlugin plugin;
    private final Map<String, RedPocket> redPocketCache;

    public RedPocketManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.redPocketCache = new ConcurrentHashMap<>();
    }

    /**
     * 创建红包
     * 注意：此方法不扣除玩家余额，调用者需要先验证余额并扣除
     */
    public RedPocket createRedPocket(UUID sender, RedPocket.RedPocketType type,
                                     double totalAmount, int count, String note) {
        String id = UUID.randomUUID().toString();
        long createdAt = System.currentTimeMillis();
        long expirationTime = plugin.getConfigManager().getExpirationTime();
        long expiresAt = expirationTime > 0 ? createdAt + (expirationTime * 1000) : 0;

        RedPocket redPocket = new RedPocket(id, sender, type, totalAmount, count, note, createdAt, expiresAt);

        // 保存到数据库
        saveRedPocketToDatabase(redPocket);

        // 添加到缓存
        redPocketCache.put(id, redPocket);

        plugin.getPluginLogger().info("创建红包: " + id + " 类型: " + type);

        return redPocket;
    }

    /**
     * 创建物品红包
     * @param player 创建红包的玩家
     * @param count 红包数量
     * @param note 备注
     * @return 创建成功的红包对象，如果失败返回 null
     */
    public RedPocket createItemRedPocket(org.bukkit.entity.Player player, int count, String note) {
        String id = UUID.randomUUID().toString();
        long createdAt = System.currentTimeMillis();
        long expirationTime = plugin.getConfigManager().getExpirationTime();
        long expiresAt = expirationTime > 0 ? createdAt + (expirationTime * 1000) : 0;

        RedPocket redPocket = new RedPocket(id, player.getUniqueId(),
            RedPocket.RedPocketType.ITEM, 0, count, note, createdAt, expiresAt);

        // 保存到数据库
        saveRedPocketToDatabase(redPocket);

        // 添加到缓存
        redPocketCache.put(id, redPocket);

        // 加载并保存物品预览
        org.bukkit.inventory.ItemStack[] items = plugin.getItemEditStorageManager().loadPlayerItems(player.getUniqueId());
        if (items != null && items.length > 0) {
            List<org.bukkit.inventory.ItemStack> itemList = new ArrayList<>();
            for (org.bukkit.inventory.ItemStack item : items) {
                if (item != null) {
                    itemList.add(item.clone());
                }
            }
            plugin.getPreviewManager().savePreview(id, itemList, expiresAt);
        }

        // 关联物品到红包
        plugin.getItemRedPocketGUI().associateRedPocket(player, id, expiresAt);

        plugin.getPluginLogger().info("创建物品红包: " + id + " 数量: " + count);

        return redPocket;
    }

    /**
     * 验证并扣除玩家余额后创建红包
     * @param player 创建红包的玩家
     * @param type 红包类型
     * @param totalAmount 总金额
     * @param count 红包数量
     * @param note 备注
     * @return 创建成功的红包对象，如果失败返回 null
     */
    public RedPocket createRedPocketWithValidation(org.bukkit.entity.Player player,
                                                    RedPocket.RedPocketType type,
                                                    double totalAmount, int count, String note) {
        // 验证金额
        if (totalAmount <= 0) {
            plugin.getMessageManager().sendError(player, "commands.create.invalid_amount");
            return null;
        }

        // 验证数量
        if (count <= 0) {
            plugin.getMessageManager().sendError(player, "commands.create.invalid_count");
            return null;
        }

        // 检查经济系统
        if (!plugin.getEconomyManager().isEnabled()) {
            plugin.getMessageManager().sendError(player, "economy.not_enabled");
            return null;
        }

        // 检查余额是否足够
        if (!plugin.getEconomyManager().hasEnough(player, totalAmount)) {
            plugin.getMessageManager().sendError(player, "commands.create.insufficient_funds");
            return null;
        }

        // 扣除金额
        if (!plugin.getEconomyManager().withdraw(player, totalAmount)) {
            plugin.getMessageManager().sendError(player, "economy.withdraw_failed");
            return null;
        }

        // 创建红包
        RedPocket redPocket = createRedPocket(player.getUniqueId(), type, totalAmount, count, note);
        return redPocket;
    }

    /**
     * 抢红包（仅计算金额，不发放）
     */
    public Optional<Double> grabRedPocket(String redPocketId, UUID claimer) {
        RedPocket redPocket = getRedPocket(redPocketId);

        if (redPocket == null) {
            plugin.getPluginLogger().debug("红包不存在: " + redPocketId);
            return Optional.empty();
        }

        if (!redPocket.isValid()) {
            plugin.getPluginLogger().debug("红包无效: " + redPocketId);
            return Optional.empty();
        }

        // 检查是否已经抢过
        if (hasClaimed(redPocketId, claimer)) {
            plugin.getPluginLogger().debug("玩家已抢过红包: " + claimer);
            return Optional.empty();
        }

        // 计算金额
        double amount = calculateAmount(redPocket);

        if (amount <= 0) {
            return Optional.empty();
        }

        // 保存记录
        RedPocketRecord record = new RedPocketRecord(
            UUID.randomUUID().toString(),
            redPocketId,
            claimer,
            amount,
            System.currentTimeMillis()
        );

        saveRecordToDatabase(record);

        plugin.getPluginLogger().info("玩家 " + claimer + " 抢到红包: " + amount);

        return Optional.of(amount);
    }

    /**
     * 抢红包并发放（带玩家对象）
     * @param redPocketId 红包ID
     * @param player 抢红包的玩家
     * @return 抢到的金额，物品红包返回1.0，如果失败返回 Optional.empty()
     */
    public Optional<Double> grabRedPocketWithPayment(String redPocketId, org.bukkit.entity.Player player) {
        RedPocket redPocket = getRedPocket(redPocketId);
        if (redPocket == null) {
            return Optional.empty();
        }

        // 检查红包类型
        if (redPocket.getType() == RedPocket.RedPocketType.ITEM) {
            // 物品红包处理
            return grabItemRedPocket(redPocketId, player);
        }

        // 金币红包处理
        // 先计算金额
        Optional<Double> amountOpt = grabRedPocket(redPocketId, player.getUniqueId());

        if (amountOpt.isEmpty()) {
            return amountOpt;
        }

        double amount = amountOpt.get();

        // 检查经济系统
        if (!plugin.getEconomyManager().isEnabled()) {
            plugin.getMessageManager().sendError(player, "economy.not_enabled");
            return Optional.empty();
        }

        // 发放金额
        if (!plugin.getEconomyManager().deposit(player, amount)) {
            plugin.getPluginLogger().severe("发放红包金额失败: 玩家=" + player.getName() + " 金额=" + amount);
            // 即使发放失败，记录已经保存，避免重复领取
            return amountOpt;
        }

        // 广播抢红包结果
        broadcastCoinRedPocketGrab(redPocket, player, amount);

        // 检查红包是否被抢完
        if (isRedPocketCompleted(redPocket)) {
            // 标记为已抢完
            redPocket.setClaimed(true);
            updateRedPocketClaimedStatus(redPocketId, true);

            // 广播红包抢完信息（显示气运最佳）
            broadcastRedPocketCompleted(redPocket);
        }

        return amountOpt;
    }

    /**
     * 抢物品红包
     * @param redPocketId 红包ID
     * @param player 抢红包的玩家
     * @return 抢到的物品数量（固定1），如果失败返回 Optional.empty()
     */
    private Optional<Double> grabItemRedPocket(String redPocketId, org.bukkit.entity.Player player) {
        RedPocket redPocket = getRedPocket(redPocketId);
        if (redPocket == null || redPocket.getType() != RedPocket.RedPocketType.ITEM) {
            return Optional.empty();
        }

        // 检查是否已经抢过
        if (hasClaimed(redPocketId, player.getUniqueId())) {
            plugin.getMessageManager().sendError(player, "gui.redpocket.grab.already_grabbed");
            return Optional.empty();
        }

        // 获取红包发送者的物品
        UUID senderUUID = redPocket.getSender();
        org.bukkit.inventory.ItemStack[] senderItems = plugin.getItemEditStorageManager().loadPlayerItems(senderUUID);

        if (senderItems == null || senderItems.length == 0) {
            plugin.getMessageManager().sendError(player, "gui.redpocket.grab.failed");
            return Optional.empty();
        }

        // 随机选择一个有物品的槽位
        java.util.Random random = new java.util.Random();
        org.bukkit.inventory.ItemStack selectedItem = null;
        int selectedSlot = -1;
        int attempts = 0;
        int maxAttempts = 100;

        while (selectedItem == null && attempts < maxAttempts) {
            int slot = random.nextInt(senderItems.length);
            org.bukkit.inventory.ItemStack item = senderItems[slot];
            if (item != null && item.getAmount() > 0) {
                selectedItem = item;
                selectedSlot = slot;
            }
            attempts++;
        }

        if (selectedItem == null) {
            plugin.getMessageManager().sendError(player, "gui.redpocket.grab.empty");
            return Optional.empty();
        }

        // 创建物品副本给玩家
        org.bukkit.inventory.ItemStack itemToGive = selectedItem.clone();
        itemToGive.setAmount(1);

        // 检查玩家背包是否有空间
        if (player.getInventory().firstEmpty() == -1) {
            plugin.getMessageManager().sendError(player, "gui.redpocket.grab.inventory_full");
            return Optional.empty();
        }

        // 减少发送者物品数量或移除
        if (selectedItem.getAmount() > 1) {
            selectedItem.setAmount(selectedItem.getAmount() - 1);
        } else {
            senderItems[selectedSlot] = null;
        }

        // 保存更新后的物品
        plugin.getItemEditStorageManager().savePlayerItems(senderUUID, senderItems,
            redPocketId, redPocket.getExpiresAt());

        // 给玩家物品
        player.getInventory().addItem(itemToGive);

        // 获取物品名称（用于显示）
        String itemName = itemToGive.getItemMeta() != null && itemToGive.getItemMeta().hasDisplayName() ?
            itemToGive.getItemMeta().getDisplayName() : itemToGive.getType().name();
        int itemAmount = itemToGive.getAmount();

        // 保存抢取记录（amount字段用于记录物品数量，这里存1）
        RedPocketRecord record = new RedPocketRecord(
            UUID.randomUUID().toString(),
            redPocketId,
            player.getUniqueId(),
            1.0,
            System.currentTimeMillis()
        );
        saveRecordToDatabase(record);

        plugin.getPluginLogger().info("玩家 " + player.getName() + " 抢到了物品红包中的物品");

        // 发送抢取成功的消息
        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
        placeholders.put("item", itemName);
        placeholders.put("amount", String.valueOf(itemAmount));
        plugin.getMessageManager().sendSuccess(player, "gui.redpocket.grab.success_item", placeholders);

        // 广播抢红包结果
        broadcastItemRedPocketGrab(redPocket, player, itemName, itemAmount);

        // 检查红包是否被抢完
        int claimedCount = getClaimedCount(redPocketId);
        if (claimedCount >= redPocket.getCount()) {
            // 移除预览数据
            plugin.getPreviewManager().removePreview(redPocketId);

            // 清除红包关联，解除物品锁定
            plugin.getItemEditStorageManager().clearRedPocketAssociation(redPocket.getSender());
        }

        return Optional.of(1.0);
    }

    /**
     * 计算红包金额
     */
    private double calculateAmount(RedPocket redPocket) {
        if (redPocket.getType() == RedPocket.RedPocketType.AVERAGE) {
            // 平分红包
            return redPocket.getTotalAmount() / redPocket.getCount();
        } else {
            // 随机红包 - 使用加权随机算法
            return calculateRandomAmount(redPocket);
        }
    }

    /**
     * 计算随机红包金额
     */
    private double calculateRandomAmount(RedPocket redPocket) {
        double totalAmount = redPocket.getTotalAmount();
        int count = redPocket.getCount();

        // 简单的随机算法
        // 实际项目中可以使用更复杂的算法保证公平性
        double minAmount = totalAmount / count * 0.1; // 最小10%
        double maxAmount = totalAmount / count * 3.0;  // 最大300%

        double amount;
        if (count == 1) {
            amount = totalAmount;
        } else {
            amount = minAmount + Math.random() * (maxAmount - minAmount);
            amount = Math.min(amount, totalAmount);
        }

        return Math.round(amount * 100.0) / 100.0;
    }

    /**
     * 获取红包
     */
    public RedPocket getRedPocket(String id) {
        // 先从缓存获取
        RedPocket redPocket = redPocketCache.get(id);

        if (redPocket == null) {
            // 从数据库加载
            redPocket = loadRedPocketFromDatabase(id);
            if (redPocket != null) {
                redPocketCache.put(id, redPocket);
            }
        }

        return redPocket;
    }

    /**
     * 从缓存获取红包
     */
    public Optional<RedPocket> getRedPocketFromCache(String id) {
        return Optional.ofNullable(redPocketCache.get(id));
    }

    /**
     * 获取红包记录列表
     */
    public List<RedPocketRecord> getRedPocketRecords(String redPocketId) {
        List<RedPocketRecord> records = new ArrayList<>();

        String sql = "SELECT * FROM redpocket_records WHERE redpocket_id = ? ORDER BY claimed_at DESC";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, redPocketId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("id"));
                map.put("redPocketId", rs.getString("redpocket_id"));
                map.put("claimer", rs.getString("claimer"));
                map.put("amount", rs.getDouble("amount"));
                map.put("claimedAt", rs.getLong("claimed_at"));

                records.add(new RedPocketRecord(map));
            }

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("获取红包记录失败: " + e.getMessage());
            e.printStackTrace();
        }

        return records;
    }

    /**
     * 检查玩家是否已抢过红包
     */
    private boolean hasClaimed(String redPocketId, UUID claimer) {
        String sql = "SELECT COUNT(*) FROM redpocket_records WHERE redpocket_id = ? AND claimer = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, redPocketId);
            stmt.setString(2, claimer.toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("检查抢取记录失败: " + e.getMessage());
        }

        return false;
    }

    /**
     * 保存红包到数据库
     */
    private void saveRedPocketToDatabase(RedPocket redPocket) {
        String sql = "INSERT INTO redpockets (id, sender, type, total_amount, count, note, created_at, expires_at, is_claimed) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, redPocket.getId());
            stmt.setString(2, redPocket.getSender().toString());
            stmt.setString(3, redPocket.getType().name());
            stmt.setDouble(4, redPocket.getTotalAmount());
            stmt.setInt(5, redPocket.getCount());
            stmt.setString(6, redPocket.getNote());
            stmt.setLong(7, redPocket.getCreatedAt());
            stmt.setLong(8, redPocket.getExpiresAt());
            stmt.setBoolean(9, redPocket.isClaimed());

            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("保存红包到数据库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存红包记录到数据库
     */
    private void saveRecordToDatabase(RedPocketRecord record) {
        String sql = "INSERT INTO redpocket_records (id, redpocket_id, claimer, amount, claimed_at) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, record.getId());
            stmt.setString(2, record.getRedPocketId());
            stmt.setString(3, record.getClaimer().toString());
            stmt.setDouble(4, record.getAmount());
            stmt.setLong(5, record.getClaimedAt());

            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("保存红包记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从数据库加载红包
     */
    private RedPocket loadRedPocketFromDatabase(String id) {
        String sql = "SELECT * FROM redpockets WHERE id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getString("id"));
                map.put("sender", rs.getString("sender"));
                map.put("type", rs.getString("type"));
                map.put("totalAmount", rs.getDouble("total_amount"));
                map.put("count", rs.getInt("count"));
                map.put("note", rs.getString("note"));
                map.put("createdAt", rs.getLong("created_at"));
                map.put("expiresAt", rs.getLong("expires_at"));
                map.put("isClaimed", rs.getBoolean("is_claimed"));

                return new RedPocket(map);
            }

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("加载红包失败: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 删除红包
     */
    public void deleteRedPocket(String id) {
        String sql = "DELETE FROM redpockets WHERE id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.executeUpdate();

            redPocketCache.remove(id);
            plugin.getPluginLogger().info("删除红包: " + id);

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("删除红包失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 向所有玩家广播红包发送消息
     */
    public void broadcastRedPocket(RedPocket redPocket) {
        org.bukkit.Bukkit.getOnlinePlayers().forEach(player -> {
            if (redPocket.getType() == RedPocket.RedPocketType.ITEM) {
                // 物品红包广播
                broadcastItemRedPocket(player, redPocket);
            } else {
                // 金币红包广播
                broadcastCoinRedPocket(player, redPocket);
            }
        });
    }

    /**
     * 广播金币红包
     */
    private void broadcastCoinRedPocket(org.bukkit.entity.Player player, RedPocket redPocket) {
        String senderName = plugin.getServer().getOfflinePlayer(redPocket.getSender()).getName();
        if (senderName == null) senderName = plugin.getMessageManager().getMessage("gui.redpocket.broadcast.unknown_player");

        // 获取类型名称
        String typeName;
        switch (redPocket.getType()) {
            case RANDOM:
                typeName = plugin.getMessageManager().getMessage("gui.redpocket.type.random");
                break;
            case AVERAGE:
                typeName = plugin.getMessageManager().getMessage("gui.redpocket.type.average");
                break;
            default:
                typeName = redPocket.getType().name();
        }

        // 构建消息前缀
        TextComponent message = new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_coin_prefix"));
        message.setColor(net.md_5.bungee.api.ChatColor.WHITE);
        message.addExtra(new TextComponent(senderName));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_coin_middle1")));
        message.addExtra(new TextComponent(String.valueOf(redPocket.getCount())));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_coin_middle2")));
        message.addExtra(new TextComponent(String.valueOf(redPocket.getTotalAmount())));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_coin_middle3")));
        message.addExtra(new TextComponent(typeName));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_coin_suffix")));

        // 添加可点击的抢红包文本
        message.addExtra(plugin.getChatClickListener().createClickableGrabText(redPocket.getId()));

        player.spigot().sendMessage(message);
    }

    /**
     * 广播金币红包抢取结果
     */
    private void broadcastCoinRedPocketGrab(RedPocket redPocket, org.bukkit.entity.Player player, double amount) {
        String senderName = plugin.getServer().getOfflinePlayer(redPocket.getSender()).getName();
        if (senderName == null) senderName = plugin.getMessageManager().getMessage("gui.redpocket.broadcast.unknown_player");

        // 构建消息：玩家 {player} 在 {sender} 的红包中抢到了 {amount} 元
        TextComponent message = new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.coin_prefix"));
        message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        message.addExtra(new TextComponent(player.getName()));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.coin_middle")));
        message.addExtra(new TextComponent(senderName));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.coin_middle2")));
        message.addExtra(new TextComponent(String.valueOf(amount)));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.coin_middle3")));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.coin_suffix")));

        // 发送给所有在线玩家
        org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.spigot().sendMessage(message));
    }

    /**
     * 广播物品红包抢取结果
     */
    private void broadcastItemRedPocketGrab(RedPocket redPocket, org.bukkit.entity.Player player, String itemName, int itemAmount) {
        String senderName = plugin.getServer().getOfflinePlayer(redPocket.getSender()).getName();
        if (senderName == null) senderName = plugin.getMessageManager().getMessage("gui.redpocket.broadcast.unknown_player");

        // 构建消息：玩家 {player} 领取了 {sender} 的物品红包 {itemName}×{itemAmount}
        TextComponent message = new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.item_prefix"));
        message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        message.addExtra(new TextComponent(player.getName()));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.item_middle")));
        message.addExtra(new TextComponent(senderName));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.item_middle2")));
        message.addExtra(new TextComponent(itemName));
        message.addExtra(new TextComponent("×"));
        message.addExtra(new TextComponent(String.valueOf(itemAmount)));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.item_suffix")));

        // 发送给所有在线玩家
        org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.spigot().sendMessage(message));
    }

    /**
     * 检查红包是否被抢完
     */
    private boolean isRedPocketCompleted(RedPocket redPocket) {
        int claimedCount = getClaimedCount(redPocket.getId());
        return claimedCount >= redPocket.getCount();
    }

    /**
     * 获取红包已抢取数量
     */
    private int getClaimedCount(String redPocketId) {
        String sql = "SELECT COUNT(*) FROM redpocket_records WHERE redpocket_id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, redPocketId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("获取红包抢取数量失败: " + e.getMessage());
        }

        return 0;
    }

    /**
     * 更新红包的抢取状态
     */
    private void updateRedPocketClaimedStatus(String redPocketId, boolean claimed) {
        String sql = "UPDATE redpockets SET is_claimed = ? WHERE id = ?";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBoolean(1, claimed);
            stmt.setString(2, redPocketId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("更新红包状态失败: " + e.getMessage());
        }
    }

    /**
     * 广播红包抢完信息（显示气运最佳）
     */
    private void broadcastRedPocketCompleted(RedPocket redPocket) {
        String senderName = plugin.getServer().getOfflinePlayer(redPocket.getSender()).getName();
        if (senderName == null) senderName = plugin.getMessageManager().getMessage("gui.redpocket.broadcast.unknown_player");

        // 获取抢到最多的玩家（气运最佳）
        Map.Entry<UUID, Double> bestLucky = getBestLuckyPlayer(redPocket.getId());

        if (bestLucky != null) {
            String bestPlayerName = plugin.getServer().getOfflinePlayer(bestLucky.getKey()).getName();
            if (bestPlayerName == null) bestPlayerName = plugin.getMessageManager().getMessage("gui.redpocket.broadcast.unknown_player");

            // 构建消息：玩家 {bestPlayer} 在 {sender} 的红包中抢到了 {amount} 元，气运爆棚！
            TextComponent message = new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.completed_prefix"));
            message.setColor(net.md_5.bungee.api.ChatColor.GOLD);
            message.addExtra(new TextComponent(bestPlayerName));
            message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.completed_middle")));
            message.addExtra(new TextComponent(senderName));
            message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.completed_middle2")));
            message.addExtra(new TextComponent(String.valueOf(bestLucky.getValue())));
            message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.completed_middle3")));
            message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.completed_suffix")));
            message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.completed_end")));

            // 发送给所有在线玩家
            org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.spigot().sendMessage(message));
        }
    }

    /**
     * 获取抢到最多的玩家（气运最佳）
     */
    private Map.Entry<UUID, Double> getBestLuckyPlayer(String redPocketId) {
        String sql = "SELECT claimer, SUM(amount) as total_amount FROM redpocket_records " +
                     "WHERE redpocket_id = ? GROUP BY claimer ORDER BY total_amount DESC LIMIT 1";

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, redPocketId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                UUID claimer = UUID.fromString(rs.getString("claimer"));
                double totalAmount = rs.getDouble("total_amount");
                return Map.entry(claimer, totalAmount);
            }

        } catch (SQLException e) {
            plugin.getPluginLogger().severe("获取气运最佳玩家失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 广播物品红包
     */
    private void broadcastItemRedPocket(org.bukkit.entity.Player player, RedPocket redPocket) {
        String senderName = plugin.getServer().getOfflinePlayer(redPocket.getSender()).getName();
        if (senderName == null) senderName = plugin.getMessageManager().getMessage("gui.redpocket.broadcast.unknown_player");

        // 构建消息前缀
        TextComponent message = new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_item_prefix"));
        message.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        message.addExtra(new TextComponent(senderName));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_item_middle")));

        // 创建可点击的类型文本
        String typeName = plugin.getMessageManager().getMessage("gui.redpocket.type.item");
        TextComponent typeComponent = new TextComponent(typeName);
        typeComponent.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        typeComponent.setBold(true);
        typeComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
            "/redpocket preview " + redPocket.getId()
        ));
        typeComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
            new net.md_5.bungee.api.chat.hover.content.Text(
                new net.md_5.bungee.api.chat.ComponentBuilder(plugin.getMessageManager().getMessage("gui.redpocket.preview.click_hint"))
                    .color(net.md_5.bungee.api.ChatColor.GREEN)
                    .create()
            )
        ));

        message.addExtra(typeComponent);
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_item_middle2")));
        message.addExtra(new TextComponent(String.valueOf(redPocket.getCount())));
        message.addExtra(new TextComponent(plugin.getMessageManager().getMessage("gui.redpocket.broadcast.send_item_middle3")));

        // 添加可点击的抢红包文本
        message.addExtra(plugin.getChatClickListener().createClickableGrabText(redPocket.getId()));

        player.spigot().sendMessage(message);
    }
}
