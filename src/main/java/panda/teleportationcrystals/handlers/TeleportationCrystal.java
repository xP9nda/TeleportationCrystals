package panda.teleportationcrystals.handlers;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.specifier.Range;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;;
import panda.teleportationcrystals.TeleportationCrystals;

import java.util.List;
import java.util.UUID;

public class TeleportationCrystal implements Listener {

    private ItemStack teleportationCrystalItem;
    private final NamespacedKey itemKey;
    private final NamespacedKey locationKey;
    private final NamespacedKey worldKey;
    private final NamespacedKey usesKey;
    private final NamespacedKey crystalUUIDKey;

    private String crystalItemName;
    private Material crystalMaterial;
    private String receivedCrystalMessage;
    private String crystalPositionUpdatedMessage;
    private String crystalNoPositionMessage;
    private String crystalTeleportMessage;
    private String reloadMessage;
    private int crystalDefaultUses;
    private final TeleportationCrystals tpCrystalsLoader;
    private List<String> locationUnsetLoreStrings;
    private List<String> locationSetLoreStrings;

    private final MiniMessage miniMsg = MiniMessage.miniMessage();

    // Constructor method
    public TeleportationCrystal(TeleportationCrystals loader) {
        tpCrystalsLoader = loader;

        reloadTeleportationCrystal();

        // Create the item keys
        this.itemKey = new NamespacedKey(loader, "item");
        this.locationKey = new NamespacedKey(loader, "location");
        this.worldKey = new NamespacedKey(loader, "world");
        this.usesKey = new NamespacedKey(loader, "uses");
        this.crystalUUIDKey = new NamespacedKey(loader, "uuid");

        setupTeleportationCrystalItem();
    }

    private void reloadTeleportationCrystal() {
        FileConfiguration config = tpCrystalsLoader.getConfig();

        // Set up the item
        // Material
        String crystalString = config.getString("crystal_item");
        crystalMaterial = Material.matchMaterial(crystalString);
        if (crystalMaterial == null) {
            tpCrystalsLoader.getSLF4JLogger().warn("'crystal_item' in config.yml is not a valid color. (Default item will be used)");
            crystalMaterial = Material.EMERALD;
        }

        // Item name
        crystalItemName = config.getString("crystal_name");
        if (crystalItemName == null || crystalItemName.isEmpty()) {
            tpCrystalsLoader.getSLF4JLogger().warn("'crystal_name' in config.yml is not a valid color. (Default name will be used)");
            crystalItemName = "<green>Teleportation Crystal";
        }

        // Messages
        receivedCrystalMessage = config.getString("crystal_command_message");
        crystalPositionUpdatedMessage = config.getString("crystal_position_updated_message");
        crystalNoPositionMessage = config.getString("crystal_no_position_message");
        crystalTeleportMessage = config.getString("crystal_teleport_message");
        locationUnsetLoreStrings = config.getStringList("crystal_lore_location_unset");
        locationSetLoreStrings = config.getStringList("crystal_lore_location_set");
        reloadMessage = config.getString("reload_message");
        crystalDefaultUses = config.getInt("crystal_default_uses");
    }

    private void setupTeleportationCrystalItem() {
        this.teleportationCrystalItem = new ItemStack(crystalMaterial);
        this.teleportationCrystalItem.editMeta(itemMeta -> {
            itemMeta.displayName(miniMsg.deserialize(crystalItemName).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            itemMeta.getPersistentDataContainer().set(this.locationKey, PersistentDataType.INTEGER_ARRAY, new int[] { 0, 0, 0 });
            itemMeta.getPersistentDataContainer().set(this.worldKey, PersistentDataType.STRING, "");
            itemMeta.getPersistentDataContainer().set(this.itemKey, PersistentDataType.STRING, "panda.teleportation_crystal");
        });
    }

    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal give <player> [uses]")
    @CommandPermission("panda.teleportationcrystals.give")
    private void onTeleportationCrystalGive(Player commandSender, @Argument("player") Player player , @Argument("uses") @Range(min = "1", max = "5000") Integer uses) {
        // Check if the uses is defined
        if (uses == null) {
            uses = crystalDefaultUses;
        }

        // Create a clone of the original item and give it a random UUID to ensure non stack-ability as well as setting the number of uses
        ItemStack newItemClone = this.teleportationCrystalItem.clone();
        Integer finalUses = uses;

        newItemClone.editMeta(itemMeta -> {
            itemMeta.getPersistentDataContainer().set(this.usesKey, PersistentDataType.INTEGER, finalUses);

            itemMeta.lore(
                    locationUnsetLoreStrings.stream().map(
                            it -> miniMsg.deserialize(
                                    it,
                                    Placeholder.unparsed("uses", String.valueOf(finalUses))
                            ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    ).toList()
            );

            itemMeta.getPersistentDataContainer().set(this.crystalUUIDKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        });

        player.getInventory().addItem(newItemClone);

        // Check if the receiver should be sent a message
        if (!receivedCrystalMessage.isEmpty()) {
            player.sendMessage(miniMsg.deserialize(
                    receivedCrystalMessage,
                    Placeholder.unparsed("player", commandSender.getName()),
                    Placeholder.unparsed("uses", String.valueOf(finalUses))
            ));
        }
    }

    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal reload")
    @CommandPermission("panda.teleportationcrystals.reload")
    private void onTeleportationCrystalReload(Player commandSender) {
        tpCrystalsLoader.getSLF4JLogger().info("Config reloaded");
        tpCrystalsLoader.reloadConfig();
        reloadTeleportationCrystal();
        setupTeleportationCrystalItem();

        // Check if the reload message should be sent
        if (!reloadMessage.isEmpty()) {
            commandSender.sendMessage(miniMsg.deserialize(reloadMessage));
        }
    }

    @EventHandler
    private void onInteract(PlayerInteractEvent event) {
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

        // Get the player and their location
        Player player = event.getPlayer();
        Location playerLocation = player.getLocation();
        int posX = playerLocation.getBlockX();
        int posY = playerLocation.getBlockY();
        int posZ = playerLocation.getBlockZ();

        // Get the remaining uses of the crystal
        int remainingUses = item.getItemMeta().getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);

        // Check if the player is sneaking
        if (player.isSneaking()) {
            // Set the teleport location
            int finalRemainingUses1 = remainingUses;

            item.editMeta(itemMeta -> {
                itemMeta.getPersistentDataContainer().set(locationKey, PersistentDataType.INTEGER_ARRAY, new int[] { posX, posY, posZ });
                itemMeta.getPersistentDataContainer().set(worldKey, PersistentDataType.STRING, playerLocation.getWorld().getName());

                itemMeta.addEnchant(Enchantment.CHANNELING, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                itemMeta.lore(
                        locationSetLoreStrings.stream().map(
                                it -> miniMsg.deserialize(
                                        it,
                                        Placeholder.unparsed("x", String.valueOf(posX)),
                                        Placeholder.unparsed("y", String.valueOf(posY)),
                                        Placeholder.unparsed("z", String.valueOf(posZ)),
                                        Placeholder.unparsed("uses", String.valueOf(finalRemainingUses1))
                                ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        ).toList()
                );
            });

            // Check if the player should be sent a message
            if (!crystalPositionUpdatedMessage.isEmpty()) {
                player.sendMessage(miniMsg.deserialize(crystalPositionUpdatedMessage));
            }

            tpCrystalsLoader.getSLF4JLogger().info("Player %s updated location of teleportation crystal to (%s) %s %s %s".formatted(player.getName(), player.getWorld().getName(), posX, posY, posZ));
            return;
        }

        // If the player is not sneaking
        // Teleport the player to the saved location
        String savedWorld = item.getItemMeta().getPersistentDataContainer().get(worldKey, PersistentDataType.STRING);
        // Check if the crystal has a saved location by checking the saved world, also check if the player should be sent a message
        if (savedWorld.isEmpty() && !crystalNoPositionMessage.isEmpty()) {
            // No saved location
            player.sendMessage(miniMsg.deserialize(crystalNoPositionMessage));
            return;
        }

        // Check if the crystal has 1 use remaining
        if (remainingUses <= 1) {
            // Set the amount of the item in the player's inventory to 0
            item.setAmount(0);
        }

        // Remove a use from the item
        remainingUses -= 1;
        int finalRemainingUses = remainingUses;

        item.editMeta(itemMeta -> {
            itemMeta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, finalRemainingUses);

            itemMeta.lore(
                    locationSetLoreStrings.stream().map(
                            it -> miniMsg.deserialize(
                                    it,
                                    Placeholder.unparsed("x", String.valueOf(posX)),
                                    Placeholder.unparsed("y", String.valueOf(posY)),
                                    Placeholder.unparsed("z", String.valueOf(posZ)),
                                    Placeholder.unparsed("uses", String.valueOf(finalRemainingUses))
                            ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    ).toList()
            );
        });

        // Saved location exists, get the location and teleport the player
        int[] savedLocation = item.getItemMeta().getPersistentDataContainer().get(locationKey, PersistentDataType.INTEGER_ARRAY);
        Location crystalTeleportLocation = new Location(Bukkit.getWorld(savedWorld), savedLocation[0], savedLocation[1], savedLocation[2]);

        player.teleportAsync(crystalTeleportLocation);
        tpCrystalsLoader.getSLF4JLogger().info("Player %s teleported to (%s) %s %s %s using a teleportation crystal".formatted(player.getName(), savedWorld, savedLocation[0], savedLocation[1], savedLocation[2]));

        if (!crystalTeleportMessage.isEmpty()) {
            player.sendMessage(miniMsg.deserialize(crystalTeleportMessage));
        }
    }
}
