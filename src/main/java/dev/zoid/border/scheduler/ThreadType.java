package dev.zoid.border.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.util.Objects;
import java.util.function.Consumer;

public enum ThreadType {
    ASYNC {
        @Override
        public ScheduledTask schedule(TaskScheduler.Builder builder, Consumer<ScheduledTask> action, Plugin plugin) {
            return builder.period() > 0
                    ? Bukkit.getAsyncScheduler().runAtFixedRate(plugin, action, builder.delay(), builder.period(), builder.unit())
                    : Bukkit.getAsyncScheduler().runDelayed(plugin, action, builder.delay(), builder.unit());
        }

        @Override
        public void execute(TaskScheduler.Builder builder, Runnable action, Plugin plugin) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> action.run());
        }
    },
    GLOBAL {
        @Override
        public ScheduledTask schedule(TaskScheduler.Builder builder, Consumer<ScheduledTask> action, Plugin plugin) {
            return builder.period() > 0
                    ? Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, action, builder.delay(), builder.period())
                    : Bukkit.getGlobalRegionScheduler().runDelayed(plugin, action, builder.delay());
        }

        @Override
        public void execute(TaskScheduler.Builder builder, Runnable action, Plugin plugin) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, action);
        }
    },
    REGION {
        @Override
        public ScheduledTask schedule(TaskScheduler.Builder builder, Consumer<ScheduledTask> action, Plugin plugin) {
            var loc = Objects.requireNonNull(builder.location());
            return builder.period() > 0
                    ? Bukkit.getRegionScheduler().runAtFixedRate(plugin, loc, action, builder.delay(), builder.period())
                    : Bukkit.getRegionScheduler().runDelayed(plugin, loc, action, builder.delay());
        }

        @Override
        public void execute(TaskScheduler.Builder builder, Runnable action, Plugin plugin) {
            var loc = builder.location();
            if (loc != null) {
                Bukkit.getRegionScheduler().execute(plugin, loc, action);
            }
        }
    },
    ENTITY {
        @Override
        public ScheduledTask schedule(TaskScheduler.Builder builder, Consumer<ScheduledTask> action, Plugin plugin) {
            var entity = Objects.requireNonNull(builder.entity());
            return builder.period() > 0
                    ? entity.getScheduler().runAtFixedRate(plugin, action, builder.retired(), builder.delay(), builder.period())
                    : entity.getScheduler().runDelayed(plugin, action, builder.retired(), builder.delay());
        }

        @Override
        public void execute(TaskScheduler.Builder builder, Runnable action, Plugin plugin) {
            var entity = builder.entity();
            if (entity != null) {
                entity.getScheduler().execute(plugin, action, builder.retired(), builder.delay());
            }
        }
    };

    public abstract ScheduledTask schedule(TaskScheduler.Builder builder, Consumer<ScheduledTask> action, Plugin plugin);
    public abstract void execute(TaskScheduler.Builder builder, Runnable action, Plugin plugin);
}