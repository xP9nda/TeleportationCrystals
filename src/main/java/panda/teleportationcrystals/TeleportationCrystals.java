package panda.teleportationcrystals;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import panda.teleportationcrystals.handlers.TeleportationCrystal;

public final class TeleportationCrystals extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info("[TeleportationCrystals] Starting up...");

        // Config
        saveDefaultConfig();

        // Commands and events
        TeleportationCrystal tpCrystal = new TeleportationCrystal(this);
        this.getServer().getPluginManager().registerEvents(tpCrystal, this);

//        try {
//            PaperCommandManager<CommandSender>
//
//        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info("[TeleportationCrystals] Shutting down...");
    }
}
