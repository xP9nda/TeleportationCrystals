package panda.teleportationcrystals;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.SimpleCommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import panda.teleportationcrystals.handlers.TeleportationCrystal;
import panda.teleportationcrystals.stats.Metrics;

public final class TeleportationCrystals extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

        // Config
        saveDefaultConfig();

        // bstats metrics
        Metrics metrics = new Metrics(this, 24287);

        // Commands and events
        TeleportationCrystal teleportationCrystal = new TeleportationCrystal(this);
        this.getServer().getPluginManager().registerEvents(teleportationCrystal, this);

        try {
            PaperCommandManager<CommandSender> commandManager = PaperCommandManager.createNative(
                    this,
                    CommandExecutionCoordinator.simpleCoordinator()
            );

            AnnotationParser<CommandSender> annotationParser = new AnnotationParser<CommandSender>(
                    commandManager,
                    CommandSender.class,
                    params -> SimpleCommandMeta.empty()
            );

            annotationParser.parse(teleportationCrystal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
