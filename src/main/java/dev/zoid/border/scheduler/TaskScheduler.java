package dev.zoid.border.scheduler;

import dev.zoid.border.BorderPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class TaskScheduler {

    private TaskScheduler() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private @NotNull ThreadType type = ThreadType.GLOBAL;
        private @Nullable Entity entity;
        private @Nullable Location location;
        private long delay = 0;
        private long period = -1;
        private @NotNull TimeUnit unit = TimeUnit.MILLISECONDS;
        private @Nullable Runnable retired;

        Builder() {
        }

        public @NotNull ThreadType type() {
            return this.type;
        }

        public Builder type(@NotNull ThreadType type) {
            this.type = type;
            return this;
        }

        public @Nullable Entity entity() {
            return this.entity;
        }

        public Builder entity(@Nullable Entity entity) {
            this.entity = entity;
            return this;
        }

        public @Nullable Location location() {
            return this.location;
        }

        public Builder location(@Nullable Location location) {
            this.location = location;
            return this;
        }

        public long delay() {
            return this.delay;
        }

        public Builder delay(long delay) {
            this.delay = delay;
            return this;
        }

        public long period() {
            return this.period;
        }

        public Builder period(long period) {
            this.period = period;
            return this;
        }

        public @NotNull TimeUnit unit() {
            return this.unit;
        }

        public Builder unit(@NotNull TimeUnit unit) {
            this.unit = unit;
            return this;
        }

        public @Nullable Runnable retired() {
            return this.retired;
        }

        public Builder retired(@Nullable Runnable retired) {
            this.retired = retired;
            return this;
        }

        public ScheduledTask run(@NotNull Runnable action) {
            return run(task -> action.run());
        }

        public ScheduledTask run(@NotNull Consumer<ScheduledTask> action) {
            return Objects.requireNonNull(type.schedule(this, action, BorderPlugin.instance));
        }

        public void execute(@NotNull Runnable action) {
            type.execute(this, action, BorderPlugin.instance);
        }
    }
}