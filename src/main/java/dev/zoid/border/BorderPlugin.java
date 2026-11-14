package dev.zoid.border;

import dev.zoid.border.commands.BorderCmd;
import dev.zoid.border.impl.BorderTicker;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public class BorderPlugin extends JavaPlugin {
    public static BorderPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        BorderTicker.init(this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            new BorderCmd(event.registrar());
        });
    }
}
