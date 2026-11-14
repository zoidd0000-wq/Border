package dev.zoid.border.impl;

import dev.zoid.border.PackSetter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class BorderTicker {

    private static final Border border = new Border();

    public static Border getBorder() {
        return border;
    }

    public static void init(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new Events(), plugin);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            border.advance();
            for (Player player : Bukkit.getOnlinePlayers()) {
                border.tick(player, player.getX(), player.getY(), player.getZ());
            }
        }, 0L, 1L);
    }

    private static class Events implements Listener {

        @EventHandler
        public void quit(PlayerQuitEvent event) {
            border.forget(event.getPlayer());
            border.show(event.getPlayer());
        }

        @EventHandler
        public void join(PlayerJoinEvent event) {
            PackSetter.set(event.getPlayer());
        }

        @EventHandler
        public void world(PlayerChangedWorldEvent event) {
            border.forget(event.getPlayer());
        }

        @EventHandler
        public void respawn(PlayerRespawnEvent event) {
            border.forget(event.getPlayer());
        }
    }
}