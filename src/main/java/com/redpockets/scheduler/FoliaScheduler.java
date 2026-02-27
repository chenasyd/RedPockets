package com.redpockets.scheduler;

import com.redpockets.RedPocketsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.concurrent.TimeUnit;

/**
 * Folia 调度器适配器
 * 为 Folia 和标准服务器提供统一的调度接口
 * 使用反射避免硬依赖 Folia API
 */
public class FoliaScheduler {

    private final RedPocketsPlugin plugin;
    private boolean isFolia;
    private final BukkitScheduler scheduler;
    private Object regionScheduler;
    private Object asyncScheduler;
    private Object globalRegionScheduler;

    public FoliaScheduler(RedPocketsPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();

        // 检测是否在Folia环境运行
        isFolia = Bukkit.getVersion().contains("Folia");

        if (isFolia) {
            try {
                Class<?> serverClass = Bukkit.getServer().getClass();
                regionScheduler = serverClass.getMethod("getRegionScheduler").invoke(Bukkit.getServer());
                asyncScheduler = serverClass.getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
                globalRegionScheduler = serverClass.getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Folia API检测失败，降级为标准模式");
                isFolia = false;
            }
        }
    }

    /**
     * 运行异步任务
     */
    public void runAsync(Runnable task) {
        if (!plugin.isEnabled()) {
            task.run();
            return;
        }

        if (isFolia && asyncScheduler != null) {
            try {
                asyncScheduler.getClass()
                    .getMethod("runNow", Plugin.class, Runnable.class)
                    .invoke(asyncScheduler, plugin, task);
            } catch (Exception e) {
                scheduler.runTaskAsynchronously(plugin, task);
            }
        } else {
            scheduler.runTaskAsynchronously(plugin, task);
        }
    }

    /**
     * 运行延迟异步任务
     */
    public void runAsyncLater(Runnable task, long delayTicks) {
        if (!plugin.isEnabled()) {
            task.run();
            return;
        }

        long delayMillis = delayTicks * 50L;

        if (isFolia && asyncScheduler != null) {
            try {
                asyncScheduler.getClass()
                    .getMethod("runDelayed", Plugin.class, Runnable.class, long.class, TimeUnit.class)
                    .invoke(asyncScheduler, plugin, task, delayMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks);
            }
        } else {
            scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    /**
     * 运行主线程任务
     */
    public void runSync(Runnable task) {
        if (isFolia) {
            runFoliaSync(task);
        } else {
            scheduler.runTask(plugin, task);
        }
    }

    /**
     * 延迟运行主线程任务（tick）
     */
    public void runSyncLater(Runnable task, long delayTicks) {
        if (isFolia) {
            runFoliaSyncLater(task, delayTicks);
        } else {
            scheduler.runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * 重复运行主线程任务
     */
    public void runSyncTimer(Runnable task, long delayTicks, long periodTicks) {
        if (isFolia) {
            runFoliaSyncTimer(task, delayTicks, periodTicks);
        } else {
            scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    /**
     * Folia 立即主线程任务
     */
    private void runFoliaSync(Runnable task) {
        try {
            if (globalRegionScheduler == null) {
                throw new IllegalStateException("GlobalRegionScheduler not initialized");
            }
            globalRegionScheduler.getClass()
                .getMethod("execute", Plugin.class, Runnable.class)
                .invoke(globalRegionScheduler, plugin, task);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Folia 主线程任务调度失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Folia 延迟主线程任务
     */
    private void runFoliaSyncLater(Runnable task, long delayTicks) {
        try {
            if (globalRegionScheduler == null) {
                throw new IllegalStateException("GlobalRegionScheduler not initialized");
            }
            long delayMillis = delayTicks * 50L;
            globalRegionScheduler.getClass()
                .getMethod("runDelayed", Plugin.class, Runnable.class, long.class, TimeUnit.class)
                .invoke(globalRegionScheduler, plugin, task, delayMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Folia 延迟主线程任务调度失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Folia 重复主线程任务
     */
    private void runFoliaSyncTimer(Runnable task, long delayTicks, long periodTicks) {
        try {
            if (globalRegionScheduler == null) {
                throw new IllegalStateException("GlobalRegionScheduler not initialized");
            }
            long delayMillis = delayTicks * 50L;
            long periodMillis = periodTicks * 50L;
            globalRegionScheduler.getClass()
                .getMethod("runAtFixedRate", Plugin.class, Runnable.class, long.class, long.class, TimeUnit.class)
                .invoke(globalRegionScheduler, plugin, task, delayMillis, periodMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Folia 定时主线程任务调度失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 运行位置相关的主线程任务
     */
    public void runAtLocation(Location location, Runnable task) {
        if (isFolia) {
            runFoliaAtLocation(location, task);
        } else {
            runSync(task);
        }
    }

    /**
     * Folia 位置相关任务
     */
    private void runFoliaAtLocation(Location location, Runnable task) {
        try {
            if (regionScheduler == null) {
                throw new IllegalStateException("RegionScheduler not initialized");
            }
            regionScheduler.getClass()
                .getMethod("execute", Plugin.class, Runnable.class, Location.class)
                .invoke(regionScheduler, plugin, task, location);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Folia 位置任务调度失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 运行实体相关的主线程任务
     */
    public void runForEntity(Entity entity, Runnable task) {
        if (isFolia) {
            runFoliaForEntity(entity, task);
        } else {
            runSync(task);
        }
    }

    /**
     * 延迟运行实体相关的主线程任务
     */
    public void runForEntityLater(Entity entity, Runnable task, long delayTicks) {
        if (isFolia) {
            runFoliaForEntityLater(entity, task, delayTicks);
        } else {
            runSyncLater(task, delayTicks);
        }
    }

    /**
     * Folia 实体相关任务
     */
    private void runFoliaForEntity(Entity entity, Runnable task) {
        try {
            Class<?> schedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);

            schedulerClass.getMethod("run", Plugin.class, Runnable.class, Object.class, long.class)
                .invoke(scheduler, plugin, task, null, 1L);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Folia 实体任务调度失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Folia 延迟实体相关任务
     */
    private void runFoliaForEntityLater(Entity entity, Runnable task, long delayTicks) {
        try {
            Class<?> schedulerClass = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);

            long delayMillis = delayTicks * 50L;
            schedulerClass.getMethod("runDelayed", Plugin.class, Runnable.class, long.class, TimeUnit.class)
                .invoke(scheduler, plugin, task, delayMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Folia 延迟实体任务调度失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 取消所有任务
     */
    public void cancelAllTasks() {
        if (!isFolia) {
            scheduler.cancelTasks(plugin);
        }
        // Folia会自动管理任务生命周期，无需手动取消
    }
}
