package panda.teleportationcrystals.handlers;

import cloud.commandframework.annotations.CommandMethod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import panda.teleportationcrystals.TeleportationCrystals;

import java.util.List;

public class TeleportationCrystal implements Listener {

    private final ItemStack teleportationCrystalItem;
    private final NamespacedKey itemKey;
    private final NamespacedKey locationKey;
    private final NamespacedKey worldKey;

    private final String crystalItemName;
    private final NamedTextColor crystalItemNameColor;
    private final String receivedCrystalMessage;
    private final String crystalPositionUpdatedMessage;
    private final String crystalNoPositionMessage;

    private FileConfiguration config;
    private TeleportationCrystals tpCrystalsLoader;

    // Constructor method
    public TeleportationCrystal(TeleportationCrystals loader) {
        config = loader.getConfig();
        tpCrystalsLoader = loader;

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

        String crystalItemNameString = (String) config.getString("crystal_name");

        if (crystalItemNameString.isEmpty()) {
            Bukkit.getLogger().warning("[TeleportationCrystals] 'crystal_name' in config.yml is empty. (Default name will be used)");
            crystalItemNameString = "Teleportation Crystal";
        }

        crystalItemName = crystalItemNameString;

        NamedTextColor crystalNameColor = convertToNamedTextColor(config.getString("crystal_name_color"));

        if (crystalNameColor == null) {
            Bukkit.getLogger().warning("[TeleportationCrystals] 'crystal_name_color' in config.yml is not a valid color. (Default color will be used)");
            crystalNameColor = NamedTextColor.BLUE;
        }

        crystalItemNameColor = crystalNameColor;

        this.teleportationCrystalItem = new ItemStack(crystalMaterial);
        this.teleportationCrystalItem.editMeta(itemMeta -> {
            itemMeta.displayName(Component.text(crystalItemName).color(crystalItemNameColor));
            itemMeta.lore(List.of(
                    Component.text("Teleport Location: Not Set").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    Component.text("Shift Right Click to set location").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                    Component.text("Right Click to teleport").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            ));

            itemMeta.getPersistentDataContainer().set(this.locationKey, PersistentDataType.INTEGER_ARRAY, new int[] { 0, 0, 0 });
            itemMeta.getPersistentDataContainer().set(this.worldKey, PersistentDataType.STRING, "");
            itemMeta.getPersistentDataContainer().set(this.itemKey, PersistentDataType.STRING, "panda.teleportation_crystal");
        });

        receivedCrystalMessage = config.getString("crystal_command_message");
        crystalPositionUpdatedMessage = config.getString("crystal_position_updated_message");
        crystalNoPositionMessage = config.getString("crystal_no_position_message");
    }

    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal <player>")
    private void onTeleportationCrystal(Player commandSender, Player player) {
        Bukkit.getLogger().info(commandSender.getName());

        player.getInventory().addItem(this.teleportationCrystalItem);

        // Check if the receiver should be sent a message
        if (!receivedCrystalMessage.isEmpty()) {
            player.sendMessage(Component.text(receivedCrystalMessage));
        }
    }

    @CommandMethod("teleportationcrystalreload|teleportcrystalreload|tpcrystalreload")
    private void onTeleportationCrystalReload(Player commandSender) {
        Bukkit.getLogger().info("reload config!!!");
        tpCrystalsLoader.reloadConfig();
    }

    @EventHandler
    private void OnInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || event.getItem() == null) {
            return;
        }

        // Check if the item has the appropriate NBT data
        ItemStack item = event.getItem();
        String itemNBT = item.getItemMeta().getPersistentDataContainer().get(this.itemKey, PersistentDataType.STRING);
        if (itemNBT == null || !itemNBT.equals("panda.teleportation_crystal")) {
            return;
        }

        event.setCancelled(true);

        // Get the player
        Player player = event.getPlayer();

        // Check if the player is sneaking
        if (player.isSneaking()) {
            // Set the teleport location
            Location playerLocation = player.getLocation();
            int posX = playerLocation.getBlockX();
            int posY = playerLocation.getBlockY();
            int posZ = playerLocation.getBlockZ();

            item.editMeta(itemMeta -> {
                itemMeta.getPersistentDataContainer().set(locationKey, PersistentDataType.INTEGER_ARRAY, new int[] { posX, posY, posZ });
                itemMeta.getPersistentDataContainer().set(worldKey, PersistentDataType.STRING, playerLocation.getWorld().getName());

                itemMeta.lore(List.of(
                        Component.text("Teleport Location: %s, %s, %s".formatted(posX, posY, posZ)).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                        Component.text("Shift Right Click to set location").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
                        Component.text("Right Click to teleport").decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                ));
            });

            // Check if the player should be sent a message
            if (!crystalPositionUpdatedMessage.isEmpty()) {
                player.sendMessage(Component.text(crystalPositionUpdatedMessage));
            }

            return;
        }

        // If the player is not sneaking
        // Teleport the player to the saved location
        String savedWorld = item.getItemMeta().getPersistentDataContainer().get(worldKey, PersistentDataType.STRING);
        // Check if the crystal has a saved location by checking the saved world, also check if the player should be sent a message
        if (savedWorld.isEmpty() && !crystalNoPositionMessage.isEmpty()) {
            // No saved location
            player.sendMessage(Component.text(crystalNoPositionMessage));
            return;
        }

        // Saved location exists, get the location and teleport the player
        int[] savedLocation = item.getItemMeta().getPersistentDataContainer().get(locationKey, PersistentDataType.INTEGER_ARRAY);
        Location crystalTeleportLocation = new Location(Bukkit.getWorld(savedWorld), savedLocation[0], savedLocation[1], savedLocation[2]);

        player.teleportAsync(crystalTeleportLocation);
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
