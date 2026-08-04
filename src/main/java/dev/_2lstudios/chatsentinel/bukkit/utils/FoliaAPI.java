package dev._2lstudios.chatsentinel.bukkit.utils;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class FoliaAPI {
    private static final Map<String, Method> CACHED_METHODS = new HashMap<String, Method>();
    private static final Map<String, Class<?>> CACHED_CLASSES = new HashMap<String, Class<?>>();
    private static final Map<String, Boolean> WARNED = new HashMap<String, Boolean>();

    private static BukkitScheduler bukkitScheduler;
    private static Object globalRegionScheduler;
    private static Object regionScheduler;
    private static Object asyncScheduler;
    private static Boolean folia;

    private FoliaAPI() {
    }

    public static synchronized void init(final Plugin plugin) {
        reset();
        cacheClasses();
        folia = Boolean.valueOf(determineFolia());
        if (folia.booleanValue()) {
            cacheMethods(plugin == null ? null : plugin.getLogger());
        }
    }

    public static synchronized void reset() {
        CACHED_METHODS.clear();
        CACHED_CLASSES.clear();
        WARNED.clear();
        bukkitScheduler = null;
        globalRegionScheduler = null;
        regionScheduler = null;
        asyncScheduler = null;
        folia = null;
    }

    private static void cacheClasses() {
        tryLoadClass("io.papermc.paper.threadedregions.RegionizedServer");
    }

    private static void tryLoadClass(final String className) {
        try {
            CACHED_CLASSES.put(className, Class.forName(className));
        } catch (final ClassNotFoundException ignored) {
            CACHED_CLASSES.put(className, null);
        }
    }

    private static boolean determineFolia() {
        final Class<?> marker = CACHED_CLASSES.get("io.papermc.paper.threadedregions.RegionizedServer");
        return marker != null && getGlobalRegionSchedulerSafe() != null && getRegionSchedulerSafe() != null && getAsyncSchedulerSafe() != null;
    }

    public static boolean isFolia() {
        if (folia != null) {
            return folia.booleanValue();
        }
        synchronized (FoliaAPI.class) {
            if (folia == null) {
                cacheClasses();
                folia = Boolean.valueOf(determineFolia());
                if (folia.booleanValue()) {
                    cacheMethods(null);
                }
            }
            return folia.booleanValue();
        }
    }

    public static boolean isOwnedByCurrentRegion(final Location location) {
        if (location == null || !isFolia()) {
            return true;
        }
        final Method method = CACHED_METHODS.get("server.isOwnedByCurrentRegion");
        final Object result = invoke(method, Bukkit.getServer(), null, "server.isOwnedByCurrentRegion", location);
        return Boolean.TRUE.equals(result);
    }

    public static <T> T callRegionOwned(final Plugin plugin, final Location location, final Callable<T> task) {
        if (task == null) {
            return null;
        }
        if (location == null || !isFolia() || isOwnedByCurrentRegion(location)) {
            return call(task);
        }
        final CompletableFuture<T> future = new CompletableFuture<T>();
        runTaskForRegion(plugin, location, new Runnable() {
            @Override
            public void run() {
                try {
                    future.complete(task.call());
                } catch (final Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            }
        });
        try {
            return future.get(30L, TimeUnit.SECONDS);
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        } catch (final ExecutionException exception) {
            throw new RuntimeException(exception.getCause());
        } catch (final TimeoutException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void runTask(final Plugin plugin, final Runnable run) {
        if (plugin == null || run == null) {
            return;
        }
        if (!isFolia()) {
            if (Bukkit.isPrimaryThread()) {
                run.run();
                return;
            }
            getBukkitSchedulerSafe().runTask(plugin, run);
            return;
        }
        final Method method = CACHED_METHODS.get("globalRegionScheduler.run");
        invoke(method, getGlobalRegionScheduler(), plugin.getLogger(), "globalRegionScheduler.run", plugin, new Consumer<Object>() {
            @Override
            public void accept(final Object ignored) {
                run.run();
            }
        });
    }

    public static void runTask(final Plugin plugin, final Consumer<Object> run) {
        if (plugin == null || run == null) {
            return;
        }
        if (!isFolia()) {
            if (Bukkit.isPrimaryThread()) {
                run.accept(null);
                return;
            }
            getBukkitSchedulerSafe().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    run.accept(null);
                }
            });
            return;
        }
        final Method method = CACHED_METHODS.get("globalRegionScheduler.run");
        invoke(method, getGlobalRegionScheduler(), plugin.getLogger(), "globalRegionScheduler.run.consumer", plugin, run);
    }

    public static void runTaskLater(final Plugin plugin, final Runnable run, final long delay) {
        if (plugin == null || run == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().runTaskLater(plugin, run, Math.max(0L, delay));
            return;
        }
        final Method method = CACHED_METHODS.get("globalRegionScheduler.runDelayed");
        invoke(method, getGlobalRegionScheduler(), plugin.getLogger(), "globalRegionScheduler.runDelayed", plugin, new Consumer<Object>() {
            @Override
            public void accept(final Object ignored) {
                run.run();
            }
        }, normalizeDelayTicks(delay));
    }

    public static void runTaskLater(final Plugin plugin, final Consumer<Object> run, final long delay) {
        if (plugin == null || run == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    run.accept(null);
                }
            }, Math.max(0L, delay));
            return;
        }
        final Method method = CACHED_METHODS.get("globalRegionScheduler.runDelayed");
        invoke(method, getGlobalRegionScheduler(), plugin.getLogger(), "globalRegionScheduler.runDelayed.consumer", plugin, run,
                normalizeDelayTicks(delay));
    }

    public static void runTaskTimer(final Plugin plugin, final Consumer<Object> run, final long delay, final long period) {
        if (plugin == null || run == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().runTaskTimer(plugin, new Runnable() {
                @Override
                public void run() {
                    run.accept(null);
                }
            }, Math.max(0L, delay), normalizePeriodTicks(period));
            return;
        }
        final Method method = CACHED_METHODS.get("globalRegionScheduler.runAtFixedRate");
        invoke(method, getGlobalRegionScheduler(), plugin.getLogger(), "globalRegionScheduler.runAtFixedRate", plugin, run,
                normalizeDelayTicks(delay), normalizePeriodTicks(period));
    }

    public static void runTaskAsync(final Plugin plugin, final Runnable run) {
        runTaskAsync(plugin, run, 0L);
    }

    public static void runTaskAsync(final Plugin plugin, final Runnable run, final long delay) {
        if (plugin == null || run == null) {
            return;
        }
        if (!isFolia()) {
            if (delay <= 0L) {
                getBukkitSchedulerSafe().runTaskAsynchronously(plugin, run);
            } else {
                getBukkitSchedulerSafe().runTaskLaterAsynchronously(plugin, run, delay);
            }
            return;
        }
        if (delay <= 0L) {
            final Method method = CACHED_METHODS.get("asyncScheduler.runNow");
            invoke(method, getAsyncScheduler(), plugin.getLogger(), "asyncScheduler.runNow", plugin, new Consumer<Object>() {
                @Override
                public void accept(final Object ignored) {
                    run.run();
                }
            });
            return;
        }
        final Method method = CACHED_METHODS.get("asyncScheduler.runDelayed");
        invoke(method, getAsyncScheduler(), plugin.getLogger(), "asyncScheduler.runDelayed", plugin, new Consumer<Object>() {
            @Override
            public void accept(final Object ignored) {
                run.run();
            }
        }, Long.valueOf(ticksToMillis(delay)), TimeUnit.MILLISECONDS);
    }

    public static void runTaskTimerAsync(final Plugin plugin, final Consumer<Object> run, final long delay, final long period) {
        if (plugin == null || run == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().runTaskTimerAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    run.accept(null);
                }
            }, Math.max(0L, delay), normalizePeriodTicks(period));
            return;
        }
        final Method method = CACHED_METHODS.get("asyncScheduler.runAtFixedRate");
        invoke(method, getAsyncScheduler(), plugin.getLogger(), "asyncScheduler.runAtFixedRate", plugin, run,
                Long.valueOf(positiveTicksToMillis(delay)), Long.valueOf(positiveTicksToMillis(period)), TimeUnit.MILLISECONDS);
    }

    public static void runTaskTimerAsync(final Plugin plugin, final Runnable runnable, final long delay, final long period) {
        if (runnable == null) {
            return;
        }
        runTaskTimerAsync(plugin, new Consumer<Object>() {
            @Override
            public void accept(final Object ignored) {
                runnable.run();
            }
        }, delay, period);
    }

    public static void runTaskForEntity(final Plugin plugin, final Entity entity, final Runnable run) {
        runTaskForEntity(plugin, entity, run, null, 0L);
    }

    public static boolean tryRunTaskForEntity(final Plugin plugin, final Entity entity, final Runnable run,
            final Runnable retired, final long delay) {
        if (plugin == null || entity == null || run == null) {
            return false;
        }
        if (!isFolia()) {
            if (delay <= 0L && Bukkit.isPrimaryThread()) {
                run.run();
                return true;
            }
            try {
                getBukkitSchedulerSafe().runTaskLater(plugin, run, Math.max(0L, delay));
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
        final Object entityScheduler = getEntityScheduler(plugin, entity);
        final Method method = getEntitySchedulerMethod(entityScheduler, "execute", Plugin.class, Runnable.class, Runnable.class, long.class);
        final Object result = invoke(method, entityScheduler, plugin.getLogger(), "entityScheduler.execute", plugin, run, retired, normalizeDelayTicks(delay));
        return Boolean.TRUE.equals(result);
    }

    public static void runTaskForEntity(final Plugin plugin, final Entity entity, final Runnable run, final Runnable retired, final long delay) {
        if (plugin == null || entity == null || run == null) {
            return;
        }
        boolean scheduled;
        if (!isFolia()) {
            if (delay <= 0L && Bukkit.isPrimaryThread()) {
                run.run();
                return;
            }
            try {
                getBukkitSchedulerSafe().runTaskLater(plugin, run, Math.max(0L, delay));
                scheduled = true;
            } catch (RuntimeException e) {
                scheduled = false;
            }
        } else {
            final Object entityScheduler = getEntityScheduler(plugin, entity);
            final Method method = getEntitySchedulerMethod(entityScheduler, "execute", Plugin.class, Runnable.class, Runnable.class, long.class);
            final Object result = invoke(method, entityScheduler, plugin.getLogger(), "entityScheduler.execute", plugin, run, retired, normalizeDelayTicks(delay));
            scheduled = Boolean.TRUE.equals(result);
        }
        if (!scheduled && retired != null) {
            retired.run();
        }
    }

    public static Object runTaskForEntityRepeatingHandle(final Plugin plugin, final Entity entity, final Consumer<Object> task,
            final Runnable retired, final long initialDelay, final long period) {
        if (plugin == null || entity == null || task == null) {
            return null;
        }
        if (!isFolia()) {
            return getBukkitSchedulerSafe().runTaskTimer(plugin, new Runnable() {
                @Override
                public void run() {
                    task.accept(null);
                }
            }, Math.max(0L, initialDelay), normalizePeriodTicks(period));
        }
        final Object entityScheduler = getEntityScheduler(plugin, entity);
        final Method method = getEntitySchedulerMethod(entityScheduler, "runAtFixedRate", Plugin.class, Consumer.class,
                Runnable.class, long.class, long.class);
        return invoke(method, entityScheduler, plugin.getLogger(), "entityScheduler.runAtFixedRate", plugin, task, retired,
                normalizeDelayTicks(initialDelay), normalizePeriodTicks(period));
    }

    public static void runTaskForEntityRepeating(final Plugin plugin, final Entity entity, final Consumer<Object> task,
            final Runnable retired, final long initialDelay, final long period) {
        runTaskForEntityRepeatingHandle(plugin, entity, task, retired, initialDelay, period);
    }

    public static void runTaskForRegion(final Plugin plugin, final World world, final int chunkX, final int chunkZ, final Runnable run) {
        if (plugin == null || world == null || run == null) {
            return;
        }
        if (!isFolia()) {
            runTask(plugin, run);
            return;
        }
        final Method method = CACHED_METHODS.get("regionScheduler.execute.world");
        invoke(method, getRegionScheduler(), plugin.getLogger(), "regionScheduler.execute.world", plugin, world,
                Integer.valueOf(chunkX), Integer.valueOf(chunkZ), run);
    }

    public static void runTaskForRegion(final Plugin plugin, final Location location, final Runnable run) {
        if (plugin == null || location == null || run == null) {
            return;
        }
        if (!isFolia()) {
            runTask(plugin, run);
            return;
        }
        if (isOwnedByCurrentRegion(location)) {
            run.run();
            return;
        }
        final Method method = CACHED_METHODS.get("regionScheduler.execute.location");
        invoke(method, getRegionScheduler(), plugin.getLogger(), "regionScheduler.execute.location", plugin, location, run);
    }

    public static void runTaskForRegion(final Plugin plugin, final Chunk chunk, final Runnable run) {
        if (chunk == null) {
            return;
        }
        runTaskForRegion(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), run);
    }

    public static void runTaskForRegionDelayed(final Plugin plugin, final Location location, final Consumer<Object> task, final long delay) {
        if (plugin == null || location == null || task == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    task.accept(null);
                }
            }, Math.max(0L, delay));
            return;
        }
        final Method method = CACHED_METHODS.get("regionScheduler.runDelayed");
        invoke(method, getRegionScheduler(), plugin.getLogger(), "regionScheduler.runDelayed", plugin, location, task,
                normalizeDelayTicks(delay));
    }

    public static void runTaskForRegionRepeating(final Plugin plugin, final Location location, final Consumer<Object> task,
            final long initialDelay, final long period) {
        if (plugin == null || location == null || task == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().runTaskTimer(plugin, new Runnable() {
                @Override
                public void run() {
                    task.accept(null);
                }
            }, Math.max(0L, initialDelay), normalizePeriodTicks(period));
            return;
        }
        final Method method = CACHED_METHODS.get("regionScheduler.runAtFixedRate");
        invoke(method, getRegionScheduler(), plugin.getLogger(), "regionScheduler.runAtFixedRate", plugin, location, task,
                normalizeDelayTicks(initialDelay), normalizePeriodTicks(period));
    }

    public static void runTaskLater(final Plugin plugin, final Location location, final Runnable run, final long delay) {
        if (plugin == null || location == null || run == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().runTaskLater(plugin, run, Math.max(0L, delay));
            return;
        }
        runTaskForRegionDelayed(plugin, location, new Consumer<Object>() {
            @Override
            public void accept(final Object ignored) {
                run.run();
            }
        }, delay);
    }

    public static void runTaskLater(final Plugin plugin, final Chunk chunk, final Runnable run, final long delay) {
        if (chunk == null) {
            return;
        }
        runTaskLater(plugin, new Location(chunk.getWorld(), chunk.getX() << 4, 0.0D, chunk.getZ() << 4), run, delay);
    }

    public static CompletableFuture<Boolean> teleportPlayer(final Plugin plugin, final Player player, final Location location,
            final boolean async) {
        return teleportPlayer(plugin, player, location, async, null);
    }

    public static CompletableFuture<Boolean> teleportPlayer(final Plugin plugin, final Player player, final Location location,
            final boolean async, final Runnable complete) {
        final CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        if (plugin == null || player == null || location == null) {
            future.complete(Boolean.FALSE);
            return future;
        }
        if (!isFolia()) {
            runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    completeTeleportFuture(future, Boolean.valueOf(player.teleport(location)), complete);
                }
            });
            return future;
        }
        if (async) {
            final Method method = CACHED_METHODS.get("player.teleportAsync");
            final Object result = invoke(method, player, plugin.getLogger(), "player.teleportAsync", location);
            if (result instanceof CompletableFuture) {
                @SuppressWarnings("unchecked")
                final CompletableFuture<Boolean> teleportFuture = (CompletableFuture<Boolean>) result;
                teleportFuture.whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
                    @Override
                    public void accept(final Boolean value, final Throwable throwable) {
                        if (complete != null) {
                            complete.run();
                        }
                    }
                });
                return teleportFuture;
            }
            future.complete(Boolean.FALSE);
            return future;
        }
        runTaskForEntity(plugin, player, new Runnable() {
            @Override
            public void run() {
                completeTeleportFuture(future, Boolean.valueOf(player.teleport(location)), complete);
            }
        }, new Runnable() {
            @Override
            public void run() {
                future.complete(Boolean.FALSE);
            }
        }, 0L);
        return future;
    }

    public static void cancelTask(final Object taskHandle) {
        if (taskHandle == null) {
            return;
        }
        if (taskHandle instanceof BukkitTask) {
            ((BukkitTask) taskHandle).cancel();
            return;
        }
        final Method method = getMethod(taskHandle.getClass(), "cancel");
        invoke(method, taskHandle, null, "taskHandle.cancel");
    }

    public static void cancelAllTasks(final Plugin plugin) {
        if (plugin == null) {
            return;
        }
        if (!isFolia()) {
            getBukkitSchedulerSafe().cancelTasks(plugin);
            return;
        }
        invoke(CACHED_METHODS.get("globalRegionScheduler.cancelTasks"), getGlobalRegionScheduler(), plugin.getLogger(),
                "globalRegionScheduler.cancelTasks", plugin);
        invoke(CACHED_METHODS.get("asyncScheduler.cancelTasks"), getAsyncScheduler(), plugin.getLogger(),
                "asyncScheduler.cancelTasks", plugin);
    }

    private static void cacheMethods(final Logger logger) {
        final Object global = getGlobalRegionSchedulerSafe();
        if (global != null) {
            getMethod(global.getClass(), "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            getMethod(global.getClass(), "run", Plugin.class, Consumer.class);
            getMethod(global.getClass(), "runDelayed", Plugin.class, Consumer.class, long.class);
            getMethod(global.getClass(), "cancelTasks", Plugin.class);
            CACHED_METHODS.put("globalRegionScheduler.runAtFixedRate", getMethod(global.getClass(), "runAtFixedRate",
                    Plugin.class, Consumer.class, long.class, long.class));
            CACHED_METHODS.put("globalRegionScheduler.run", getMethod(global.getClass(), "run", Plugin.class, Consumer.class));
            CACHED_METHODS.put("globalRegionScheduler.runDelayed", getMethod(global.getClass(), "runDelayed", Plugin.class,
                    Consumer.class, long.class));
            CACHED_METHODS.put("globalRegionScheduler.cancelTasks", getMethod(global.getClass(), "cancelTasks", Plugin.class));
        }

        final Object region = getRegionSchedulerSafe();
        if (region != null) {
            CACHED_METHODS.put("regionScheduler.execute.world", getMethod(region.getClass(), "execute", Plugin.class,
                    World.class, int.class, int.class, Runnable.class));
            CACHED_METHODS.put("regionScheduler.execute.location", getMethod(region.getClass(), "execute", Plugin.class,
                    Location.class, Runnable.class));
            CACHED_METHODS.put("regionScheduler.runAtFixedRate", getMethod(region.getClass(), "runAtFixedRate", Plugin.class,
                    Location.class, Consumer.class, long.class, long.class));
            CACHED_METHODS.put("regionScheduler.runDelayed", getMethod(region.getClass(), "runDelayed", Plugin.class,
                    Location.class, Consumer.class, long.class));
        }

        CACHED_METHODS.put("entity.getScheduler", getMethod(Entity.class, "getScheduler"));
        CACHED_METHODS.put("player.teleportAsync", getMethod(Player.class, "teleportAsync", Location.class));

        final Object async = getAsyncSchedulerSafe();
        if (async != null) {
            CACHED_METHODS.put("asyncScheduler.runNow", getMethod(async.getClass(), "runNow", Plugin.class, Consumer.class));
            CACHED_METHODS.put("asyncScheduler.runDelayed", getMethod(async.getClass(), "runDelayed", Plugin.class,
                    Consumer.class, long.class, TimeUnit.class));
            CACHED_METHODS.put("asyncScheduler.runAtFixedRate", getMethod(async.getClass(), "runAtFixedRate", Plugin.class,
                    Consumer.class, long.class, long.class, TimeUnit.class));
            CACHED_METHODS.put("asyncScheduler.cancelTasks", getMethod(async.getClass(), "cancelTasks", Plugin.class));
        }

        CACHED_METHODS.put("server.isOwnedByCurrentRegion", getMethod(Server.class, "isOwnedByCurrentRegion", Location.class));
        if (logger != null && isFolia() && CACHED_METHODS.get("entity.getScheduler") == null) {
            warnOnce(logger, "entity.getScheduler.missing", "Folia entity scheduler method unavailable", null);
        }
    }

    private static BukkitScheduler getBukkitSchedulerSafe() {
        if (bukkitScheduler == null) {
            bukkitScheduler = Bukkit.getScheduler();
        }
        return bukkitScheduler;
    }

    private static Object getGlobalRegionSchedulerSafe() {
        if (globalRegionScheduler != null) {
            return globalRegionScheduler;
        }
        try {
            final Method method = Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler");
            globalRegionScheduler = method.invoke(Bukkit.getServer());
            return globalRegionScheduler;
        } catch (final Throwable ignored) {
            return null;
        }
    }

    private static Object getRegionSchedulerSafe() {
        if (regionScheduler != null) {
            return regionScheduler;
        }
        try {
            final Method method = Bukkit.getServer().getClass().getMethod("getRegionScheduler");
            regionScheduler = method.invoke(Bukkit.getServer());
            return regionScheduler;
        } catch (final Throwable ignored) {
            return null;
        }
    }

    private static Object getAsyncSchedulerSafe() {
        if (asyncScheduler != null) {
            return asyncScheduler;
        }
        try {
            final Method method = Bukkit.getServer().getClass().getMethod("getAsyncScheduler");
            asyncScheduler = method.invoke(Bukkit.getServer());
            return asyncScheduler;
        } catch (final Throwable ignored) {
            return null;
        }
    }

    private static Object getGlobalRegionScheduler() {
        return getGlobalRegionSchedulerSafe();
    }

    private static Object getRegionScheduler() {
        return getRegionSchedulerSafe();
    }

    private static Object getAsyncScheduler() {
        return getAsyncSchedulerSafe();
    }

    private static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        if (clazz == null || methodName == null) {
            return null;
        }
        final String key = methodKey(clazz, methodName, parameterTypes);
        if (CACHED_METHODS.containsKey(key)) {
            return CACHED_METHODS.get(key);
        }
        try {
            final Method method = clazz.getMethod(methodName, parameterTypes);
            CACHED_METHODS.put(key, method);
            return method;
        } catch (final NoSuchMethodException exception) {
            CACHED_METHODS.put(key, null);
            return null;
        }
    }

    private static Object invoke(final Method method, final Object object, final Logger logger, final String warningKey,
            final Object... args) {
        if (method == null || object == null) {
            warnOnce(logger, warningKey + ".missing", "Folia scheduler method unavailable: " + warningKey, null);
            return null;
        }
        try {
            return method.invoke(object, args);
        } catch (final Throwable throwable) {
            warnOnce(logger, warningKey, "Folia scheduler invocation failed: " + warningKey, throwable);
            return null;
        }
    }

    private static void warnOnce(final Logger logger, final String key, final String message, final Throwable throwable) {
        if (logger == null || key == null || WARNED.containsKey(key)) {
            return;
        }
        WARNED.put(key, Boolean.TRUE);
        if (throwable == null) {
            logger.warning(message);
        } else {
            logger.warning(message + ": " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private static Object getEntityScheduler(final Plugin plugin, final Entity entity) {
        final Method method = CACHED_METHODS.get("entity.getScheduler");
        return invoke(method, entity, plugin == null ? null : plugin.getLogger(), "entity.getScheduler");
    }

    private static Method getEntitySchedulerMethod(final Object entityScheduler, final String methodName,
            final Class<?>... parameterTypes) {
        if (entityScheduler == null) {
            return null;
        }
        return getMethod(entityScheduler.getClass(), methodName, parameterTypes);
    }

    private static <T> T call(final Callable<T> task) {
        try {
            return task.call();
        } catch (final RuntimeException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void completeTeleportFuture(final CompletableFuture<Boolean> future, final Boolean result,
            final Runnable complete) {
        future.complete(result);
        if (complete != null) {
            complete.run();
        }
    }

    private static String methodKey(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        final StringBuilder builder = new StringBuilder(clazz.getName()).append('#').append(methodName);
        if (parameterTypes != null) {
            for (Class<?> parameterType : parameterTypes) {
                builder.append('#').append(parameterType == null ? "null" : parameterType.getName());
            }
        }
        return builder.toString();
    }

    private static long normalizeDelayTicks(final long delay) {
        return delay <= 0L ? 1L : delay;
    }

    private static long normalizePeriodTicks(final long period) {
        return period <= 0L ? 1L : period;
    }

    private static long ticksToMillis(final long ticks) {
        return Math.max(0L, ticks) * 50L;
    }

    private static long positiveTicksToMillis(final long ticks) {
        return Math.max(50L, ticksToMillis(normalizeDelayTicks(ticks)));
    }
}
