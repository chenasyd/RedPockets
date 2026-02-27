package com.redpockets.economy;

import com.redpockets.RedPocketsPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * 经济管理器
 * 使用 Vault API 进行经济操作
 */
public class EconomyManager {

    private final RedPocketsPlugin plugin;
    private Economy economy;
    private boolean enabled;

    public EconomyManager(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    /**
     * 初始化经济系统
     */
    public boolean initialize() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getPluginLogger().warning("未安装 Vault 插件，经济功能将不可用");
            return false;
        }

        try {
            economy = Bukkit.getServicesManager().load(Economy.class);
            if (economy == null) {
                plugin.getPluginLogger().warning("未找到经济插件，请安装支持 Vault 的经济插件");
                return false;
            }

            enabled = true;
            plugin.getPluginLogger().info("经济系统已成功加载: " + economy.getName());
            return true;

        } catch (Exception e) {
            plugin.getPluginLogger().severe("初始化经济系统失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查经济系统是否可用
     */
    public boolean isEnabled() {
        return enabled && economy != null;
    }

    /**
     * 获取玩家余额
     */
    public double getBalance(Player player) {
        if (!isEnabled()) {
            return 0;
        }

        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("获取玩家余额失败: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 检查玩家是否有足够的余额
     */
    public boolean hasEnough(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }

        try {
            return economy.has(player, amount);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("检查玩家余额失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从玩家账户扣除金额
     */
    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) {
            plugin.getPluginLogger().warning("经济系统未启用，无法扣除金额");
            return false;
        }

        if (amount <= 0) {
            plugin.getPluginLogger().warning("扣除金额必须大于0");
            return false;
        }

        try {
            economy.withdrawPlayer(player, amount);
            plugin.getPluginLogger().info("从玩家 " + player.getName() + " 扣除 " + amount + " 金币");
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("扣除金额时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 给玩家账户增加金额
     */
    public boolean deposit(Player player, double amount) {
        if (!isEnabled()) {
            plugin.getPluginLogger().warning("经济系统未启用，无法增加金额");
            return false;
        }

        if (amount <= 0) {
            plugin.getPluginLogger().warning("增加金额必须大于0");
            return false;
        }

        try {
            economy.depositPlayer(player, amount);
            plugin.getPluginLogger().info("给玩家 " + player.getName() + " 增加 " + amount + " 金币");
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("增加金额时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 给离线玩家账户增加金额
     */
    public boolean depositOffline(OfflinePlayer player, double amount) {
        if (!isEnabled()) {
            plugin.getPluginLogger().warning("经济系统未启用，无法增加金额");
            return false;
        }

        if (amount <= 0) {
            plugin.getPluginLogger().warning("增加金额必须大于0");
            return false;
        }

        try {
            economy.depositPlayer(player, amount);
            plugin.getPluginLogger().info("给离线玩家 " + player.getName() + " 增加 " + amount + " 金币");
            return true;
        } catch (Exception e) {
            plugin.getPluginLogger().severe("增加金额时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取货币名称（单数形式）
     */
    public String currencyNameSingular() {
        if (!isEnabled()) {
            return "金币";
        }
        return economy.currencyNameSingular();
    }

    /**
     * 获取货币名称（复数形式）
     */
    public String currencyNamePlural() {
        if (!isEnabled()) {
            return "金币";
        }
        return economy.currencyNamePlural();
    }

    /**
     * 格式化金额显示
     */
    public String formatAmount(double amount) {
        if (!isEnabled()) {
            return String.format("%.2f 金币", amount);
        }
        return economy.format(amount);
    }

    /**
     * 获取 Economy 实例（用于其他扩展）
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * 重置经济系统
     */
    public void reload() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            enabled = false;
            economy = null;
            plugin.getPluginLogger().warning("Vault 插件未安装");
            return;
        }

        economy = Bukkit.getServicesManager().load(Economy.class);
        if (economy != null) {
            enabled = true;
            plugin.getPluginLogger().info("经济系统已重新加载: " + economy.getName());
        } else {
            enabled = false;
            plugin.getPluginLogger().warning("未找到经济插件");
        }
    }
}
