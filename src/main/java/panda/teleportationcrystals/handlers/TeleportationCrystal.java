package panda.teleportationcrystals.handlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import panda.teleportationcrystals.TeleportationCrystals;

public class TeleportationCrystal implements Listener {

    private final ItemStack teleportationCrystalItem;
    private final NamespacedKey itemKey;
    private final NamespacedKey locationKey;
    private final NamespacedKey worldKey;

    private final Material crystalItemMaterial;
    private final String crystalItemName;
    private final NamedTextColor crystalItemNameColor;

    private FileConfiguration config;

    // Constructor method
    public TeleportationCrystal(TeleportationCrystals loader) {
        config = loader.getConfig();

        // Create the item keys
        this.itemKey = new NamespacedKey(loader, "item");
        this.locationKey = new NamespacedKey(loader, "location");
        this.worldKey = new NamespacedKey(loader, "world");

        // Set up the item
        String crystalString = (String) config.getString("crystal_item");
        Material crystalMaterial = Material.matchMaterial(crystalString);

        if (crystalMaterial == null) {
            Bukkit.getLogger().warning("[TeleportationCrystals] 'crystal_item' in config.yml is not a valid item. (Default item will be used)");
            crystalMaterial = Material.EMERALD;
        }

        crystalItemMaterial = crystalMaterial;

        crystalItemName = (String) config.getString("crystal_name");;
        NamedTextColor crystalNameColor = convertToNamedTextColor((String) config.getString("crystal_name_color"));

        if (crystalNameColor == null) {
            Bukkit.getLogger().warning("[TeleportationCrystals] 'crystal_name_color' in config.yml is not a valid color. (Default color will be used)");
            crystalNameColor = NamedTextColor.BLUE;
        }

        crystalItemNameColor = crystalNameColor;

        this.teleportationCrystalItem = new ItemStack(crystalItemMaterial);
        this.teleportationCrystalItem.editMeta(itemMeta -> {
            itemMeta.displayName(Component.text(crystalItemName).color(crystalItemNameColor));
        });

    }

    private NamedTextColor convertToNamedTextColor(String colorString) {
        // Convert the string to a NamedTextColor object
        try {
            return NamedTextColor.NAMES.value(colorString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
