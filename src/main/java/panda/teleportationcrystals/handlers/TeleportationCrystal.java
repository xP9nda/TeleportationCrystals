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
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
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
    private final NamespacedKey pitchKey;
    private final NamespacedKey yawKey;
    private final NamespacedKey infiniteUsesKey;
    private final NamespacedKey crystalUUIDKey;
    private final NamespacedKey crystalCraftingRecipeKey;

    private String crystalItemName;
    private boolean teleportationCrystalCraftingEnabled;
    private ShapedRecipe teleportationCrystalRecipe;
    private Material crystalMaterial;
    private String receivedCrystalMessage;
    private String crystalPositionUpdatedMessage;
    private String crystalNoPositionMessage;
    private String crystalTeleportMessage;
    private String reloadMessage;
    private String invalidUsesMessage;
    private int crystalDefaultUses;
    private final TeleportationCrystals tpCrystalsLoader;
    private List<String> locationUnsetLoreStrings;
    private List<String> locationSetLoreStrings;
    private List<String> crystalRecipeStrings;

    private String receiveSoundString;
    private String setLocationSoundString;
    private String teleportSoundString;
    private String teleportSoundFailString;
    private String breakSoundString;

    private Sound receiveSound;
    private Sound setLocationSound;
    private Sound teleportSound;
    private Sound teleportSoundFail;
    private Sound breakSound;

    private final MiniMessage miniMsg = MiniMessage.miniMessage();

    // Constructor method
    public TeleportationCrystal(TeleportationCrystals loader) {
        tpCrystalsLoader = loader;

        // Create the item keys
        this.itemKey = new NamespacedKey(loader, "item");
        this.locationKey = new NamespacedKey(loader, "location");
        this.worldKey = new NamespacedKey(loader, "world");
        this.usesKey = new NamespacedKey(loader, "uses");
        this.pitchKey = new NamespacedKey(loader, "pitch");
        this.yawKey = new NamespacedKey(loader, "yaw");
        this.infiniteUsesKey = new NamespacedKey(loader, "infinite_uses");
        this.crystalUUIDKey = new NamespacedKey(loader, "uuid");
        this.crystalCraftingRecipeKey = new NamespacedKey(loader, "panda.teleportationcrystalrecipe");

        reloadTeleportationCrystalConfig();
        updateTeleportationCrystalItemStack(crystalDefaultUses, false);
    }

    private void reloadTeleportationCrystalConfig() {
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
            crystalItemName = "<#75A1BF>Teleportation Crystal";
        }

        // Config options
        receivedCrystalMessage = config.getString("crystal_command_message");
        crystalPositionUpdatedMessage = config.getString("crystal_position_updated_message");
        crystalNoPositionMessage = config.getString("crystal_no_position_message");
        crystalTeleportMessage = config.getString("crystal_teleport_message");
        locationUnsetLoreStrings = config.getStringList("crystal_lore_location_unset");
        locationSetLoreStrings = config.getStringList("crystal_lore_location_set");
        reloadMessage = config.getString("reload_message");
        invalidUsesMessage = config.getString("invalid_uses_provided_message");
        crystalDefaultUses = config.getInt("crystal_default_uses");
        crystalRecipeStrings = config.getStringList("crystal_recipe");
        teleportationCrystalCraftingEnabled = config.getBoolean("crystal_recipe_enabled");

        receiveSoundString = config.getString("sound_receive");
        setLocationSoundString = config.getString("sound_setting_location");
        teleportSoundString = config.getString("sound_teleporting");
        teleportSoundFailString = config.getString("sound_teleporting_fail");
        breakSoundString = config.getString("sound_crystal_breaks");

        receiveSound = Sound.valueOf(receiveSoundString.toUpperCase());
        setLocationSound = org.bukkit.Sound.valueOf(setLocationSoundString.toUpperCase());
        teleportSound = org.bukkit.Sound.valueOf(teleportSoundString.toUpperCase());
        teleportSoundFail = org.bukkit.Sound.valueOf(teleportSoundFailString.toUpperCase());
        breakSound = org.bukkit.Sound.valueOf(breakSoundString.toUpperCase());

        // Check sound effect validity
        if (receiveSound == null && !receiveSoundString.isEmpty()) {
            tpCrystalsLoader.getSLF4JLogger().warn("'receive_sound' in config.yml is not a valid sound. (Default sound will be used)");
            receiveSound = Sound.ENTITY_ITEM_PICKUP;
        }

        if (setLocationSound == null && !setLocationSoundString.isEmpty()) {
            tpCrystalsLoader.getSLF4JLogger().warn("'sound_setting_location' in config.yml is not a valid sound. (Default sound will be used)");
            setLocationSound = Sound.BLOCK_ENCHANTMENT_TABLE_USE;
        }

        if (teleportSound == null && !teleportSoundString.isEmpty()) {
            tpCrystalsLoader.getSLF4JLogger().warn("'sound_teleporting' in config.yml is not a valid sound. (Default sound will be used)");
            teleportSound = Sound.ITEM_CHORUS_FRUIT_TELEPORT;
        }

        if (teleportSoundFail == null && !teleportSoundFailString.isEmpty()) {
            tpCrystalsLoader.getSLF4JLogger().warn("'sound_teleporting' in config.yml is not a valid sound. (Default sound will be used)");
            teleportSoundFail = Sound.BLOCK_STONE_FALL;
        }

        if (breakSound == null && !breakSoundString.isEmpty()) {
            tpCrystalsLoader.getSLF4JLogger().warn("'sound_crystal_breaks' in config.yml is not a valid sound. (Default sound will be used)");
            breakSound = Sound.ENTITY_ENDER_EYE_DEATH;
        }
    }

    // Update the crystal item and give it a random UUID to ensure non stack-ability as well as set the number of uses
    private void updateTeleportationCrystalItemStack(int uses, boolean infiniteUses) {
        this.teleportationCrystalItem = new ItemStack(crystalMaterial);

        String usesString = "";
        if (infiniteUses) {
            usesString = "infinite";
        } else {
            usesString = String.valueOf(uses);
        }
        final String finalUsesString = usesString;

        this.teleportationCrystalItem.editMeta(itemMeta -> {
            itemMeta.displayName(miniMsg.deserialize(crystalItemName).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            itemMeta.getPersistentDataContainer().set(this.locationKey, PersistentDataType.INTEGER_ARRAY, new int[] { 0, 0, 0 });
            itemMeta.getPersistentDataContainer().set(this.worldKey, PersistentDataType.STRING, "");
            itemMeta.getPersistentDataContainer().set(this.itemKey, PersistentDataType.STRING, "panda.teleportation_crystal");

            itemMeta.getPersistentDataContainer().set(this.usesKey, PersistentDataType.INTEGER, uses);
            itemMeta.getPersistentDataContainer().set(this.infiniteUsesKey, PersistentDataType.BOOLEAN, infiniteUses);
            itemMeta.getPersistentDataContainer().set(this.crystalUUIDKey, PersistentDataType.STRING, UUID.randomUUID().toString());

            itemMeta.lore(
                    locationUnsetLoreStrings.stream().map(
                            it -> miniMsg.deserialize(
                                    it,
                                    Placeholder.unparsed("uses", finalUsesString)
                            ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    ).toList()
            );
        });

        // Setup crafting recipe
        if (teleportationCrystalCraftingEnabled) {
            // If there is already a recipe, remove it before updating it to this new one
            if (teleportationCrystalRecipe != null) {
                tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
            }

            teleportationCrystalRecipe = new ShapedRecipe(this.crystalCraftingRecipeKey, this.teleportationCrystalItem);
            teleportationCrystalRecipe.shape("012", "345", "678");

            for ( int i = 0 ; i < crystalRecipeStrings.toArray().length ; i++ ) {
                String currentRecipeItem = crystalRecipeStrings.get(i);

                // Current recipe item is defined as nothing
                if (currentRecipeItem.equals("tpcrystal_empty_item")) {
                    continue;
                }

                // Current recipe item is a material
                Material currentRecipeItemMaterial = Material.matchMaterial(crystalRecipeStrings.get(i));
                if (currentRecipeItemMaterial == null) {
                    tpCrystalsLoader.getSLF4JLogger().warn("Recipe material '%s' in 'crystals_recipe' in config.yml is an invalid item. Recipe will not be added."
                            .formatted(currentRecipeItem)
                    );
                    return;
                }

                String indexString = Integer.toString(i);
                char recipeCharacterIdentifier = indexString.charAt(0);
                teleportationCrystalRecipe.setIngredient(recipeCharacterIdentifier, currentRecipeItemMaterial);
            }

            // Add the recipe
            tpCrystalsLoader.getServer().addRecipe(teleportationCrystalRecipe);

        // If the recipe should be disabled, remove it
        } else {
            // Remove the recipe if it exists
            if (teleportationCrystalRecipe != null) {
                tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
            }
        }
    }

    // Command for giving players teleportation crystals
    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal give <player> [uses]")
    @CommandPermission("panda.teleportationcrystals.give")
    private void onTeleportationCrystalGive(Player commandSender, @Argument("player") Player player , @Argument("uses") String uses) {
        int usesInt;
        boolean infiniteUses = false;

        // Check if the uses is defined
        if (uses == null || uses.isEmpty()) {
            usesInt = crystalDefaultUses;

        // If the uses is a valid number, set the uses to the provided number
        } else {
            // If the uses should be infinite
            if (uses.equals("infinite") || uses.equals("inf")) {
                infiniteUses = true;
                usesInt = 0;

            // Check if the uses is a valid number
            } else {
                try {
                    usesInt = Integer.parseInt(uses);
                } catch (NumberFormatException e) {
                    // If it's not valid, send the player a message and return
                    commandSender.sendMessage(miniMsg.deserialize(invalidUsesMessage));
                    return;
                }
            }
        }

        // Remove the old crafting recipe
        if (teleportationCrystalCraftingEnabled) {
            tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
        }

        // Update the itemstack and recipe to the new uses and infinite uses
        updateTeleportationCrystalItemStack(usesInt, infiniteUses);

        // Add the crystal to the player's inventory
        player.getInventory().addItem(this.teleportationCrystalItem);

        // Check if the receiver should be sent a message
        if (!receivedCrystalMessage.isEmpty()) {
            String usesMessage = "";

            if (infiniteUses) {
                usesMessage = "infinite";
            } else {
                usesMessage = String.valueOf(usesInt);
            }

            player.sendMessage(miniMsg.deserialize(
                    receivedCrystalMessage,
                    Placeholder.unparsed("player", commandSender.getName()),
                    Placeholder.unparsed("uses", usesMessage)
            ));
        }

        // Remove the old crafting recipe
        if (teleportationCrystalCraftingEnabled) {
            tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
        }

        // Update the itemstack and recipe back to the default uses
        updateTeleportationCrystalItemStack(crystalDefaultUses, false);

        // Sound effect
        if (!receiveSoundString.isEmpty()) {
            player.playSound(player.getLocation(), receiveSound, 1, 1);
        }
    }

    // Command for reloading the system
    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal reload")
    @CommandPermission("panda.teleportationcrystals.reload")
    private void onTeleportationCrystalReload(Player commandSender) {
        tpCrystalsLoader.getSLF4JLogger().info("Config reloaded");
        tpCrystalsLoader.reloadConfig();
        reloadTeleportationCrystalConfig();
        updateTeleportationCrystalItemStack(crystalDefaultUses, false);

        // Check if the reload message should be sent
        if (!reloadMessage.isEmpty()) {
            commandSender.sendMessage(miniMsg.deserialize(reloadMessage));
        }
    }

    // Command for plugin info
    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal info|information")
    private void onTeleportationCrystalInformation(Player commandSender) {
        String infoMessage = "<#89C9B8><b>TeleportationCrystals</b>";
        infoMessage += "<#75A1BF> by xP9nda\n\n";
        infoMessage += "<#89C9B8>This server is running version <#75A1BF><version>\n";
        infoMessage += "<#89C9B8>Source code is available on <click:open_url:'https://github.com/xP9nda/TeleportationCrystals'><u>GitHub</u></click>";
        infoMessage += " and can be downloaded on <click:open_url:'https://modrinth.com/plugin/teleportationcrystals'><u>Modrinth</u></click>";

        commandSender.sendMessage(
            miniMsg.deserialize(
                infoMessage,
                Placeholder.unparsed("version", tpCrystalsLoader.getPluginMeta().getVersion())
            )
        );
    }

    // Check craft events for crystal crafting
    @EventHandler
    private void onCraftEvent(PrepareItemCraftEvent event) {
        // Check if the craft has a valid recipe and result
        Recipe craftingRecipe = event.getRecipe();
        if (craftingRecipe == null) {
            return;
        }

        ItemStack craftingResult = craftingRecipe.getResult();

        // Check if the result is the same as the item stack
        if (!craftingResult.isSimilar(teleportationCrystalRecipe.getResult())) {
            return;
        }

        // Remove the old crafting recipe
        if (teleportationCrystalCraftingEnabled) {
            tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
        }
        // Update the itemstack and recipe
        updateTeleportationCrystalItemStack(crystalDefaultUses, false);
    }

    // Check player interactions for crystal usage
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
        int remainingUses = crystalDefaultUses;
        // Check if the item has the uses key
        if (item.getItemMeta().getPersistentDataContainer().has(usesKey, PersistentDataType.INTEGER)) {
            remainingUses = item.getItemMeta().getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
        }

        // Get whether the crystal has infinite uses
        boolean infiniteUses = false;
        // If the item does not have the infinite uses key, set it to false
        if (item.getItemMeta().getPersistentDataContainer().has(infiniteUsesKey, PersistentDataType.BOOLEAN)) {
            infiniteUses = item.getItemMeta().getPersistentDataContainer().get(infiniteUsesKey, PersistentDataType.BOOLEAN);
        }

        // Check if the player is sneaking
        if (player.isSneaking()) {
            // Work out the description text for number of uses remaining
            String usesMessage = "";

            if (infiniteUses) {
                usesMessage = "infinite";
            } else {
                usesMessage = String.valueOf(remainingUses);
            }

            final String finalUsesMessage = usesMessage;

            // Edit the item's metadata
            item.editMeta(itemMeta -> {
                // Set the new location, world, pitch, yaw
                itemMeta.getPersistentDataContainer().set(locationKey, PersistentDataType.INTEGER_ARRAY, new int[] { posX, posY, posZ });
                itemMeta.getPersistentDataContainer().set(worldKey, PersistentDataType.STRING, playerLocation.getWorld().getName());
                itemMeta.getPersistentDataContainer().set(pitchKey, PersistentDataType.FLOAT, playerLocation.getPitch());
                itemMeta.getPersistentDataContainer().set(yawKey, PersistentDataType.FLOAT, playerLocation.getYaw());

                // Make the item glow
                itemMeta.addEnchant(Enchantment.CHANNELING, 1, true);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

                // Set up the item lore
                itemMeta.lore(
                        locationSetLoreStrings.stream().map(
                                it -> miniMsg.deserialize(
                                        it,
                                        Placeholder.unparsed("x", String.valueOf(posX)),
                                        Placeholder.unparsed("y", String.valueOf(posY)),
                                        Placeholder.unparsed("z", String.valueOf(posZ)),
                                        Placeholder.unparsed("uses", finalUsesMessage),
                                        Placeholder.unparsed("world", playerLocation.getWorld().getName())
                                ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        ).toList()
                );
            });

            // Check if the player should be sent a message
            if (!crystalPositionUpdatedMessage.isEmpty()) {
                player.sendMessage(miniMsg.deserialize(crystalPositionUpdatedMessage));
            }

            // Sound effect
            if (!setLocationSoundString.isEmpty()) {
                player.playSound(playerLocation, setLocationSound, 1, 1);
            }

            // Debug
            tpCrystalsLoader.getSLF4JLogger().info("Player %s updated location of teleportation crystal to (%s) %s %s %s".formatted(player.getName(), player.getWorld().getName(), posX, posY, posZ));
            return;
        }

        // If the player is not sneaking
        // Teleport the player to the saved location
        String savedWorld = item.getItemMeta().getPersistentDataContainer().get(worldKey, PersistentDataType.STRING);

        // Check if the crystal has a saved location by checking the saved world, also check if the player should be sent a message
        assert savedWorld != null;
        if (savedWorld.isEmpty() && !crystalNoPositionMessage.isEmpty()) {
            // No saved location
            player.sendMessage(miniMsg.deserialize(crystalNoPositionMessage));

            // Sound effect
            if (!teleportSoundFailString.isEmpty()) {
                player.playSound(playerLocation, teleportSoundFail, 1, 1);
            }

            return;
        }

        // If the crystal does not have infinite uses, then do logic to remove a use
        if (!infiniteUses) {
            // Check if the crystal has 1 use remaining
            if (remainingUses <= 1) {
                // Set the amount of the item in the player's inventory to 0
                item.setAmount(0);

                // Sound effect
                if (!breakSoundString.isEmpty()) {
                    player.playSound(playerLocation, breakSound, 1, 1);
                }
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
                                        Placeholder.unparsed("uses", String.valueOf(finalRemainingUses)),
                                        Placeholder.unparsed("world", savedWorld)
                                ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                        ).toList()
                );
            });
        }

        // Saved location exists, get the location and teleport the player
        int[] savedLocation = item.getItemMeta().getPersistentDataContainer().get(locationKey, PersistentDataType.INTEGER_ARRAY);
        assert savedLocation != null;
        Location crystalTeleportLocation = new Location(Bukkit.getWorld(savedWorld), savedLocation[0], savedLocation[1], savedLocation[2]);

        // Get the saved pitch and yaw and update the location should the keys exist
        // Check if the item has the pitch key
        if (item.getItemMeta().getPersistentDataContainer().has(pitchKey, PersistentDataType.FLOAT)) {
            float pitch = item.getItemMeta().getPersistentDataContainer().get(pitchKey, PersistentDataType.FLOAT);
            crystalTeleportLocation.setPitch(pitch);
        }

        // Check if the item has the yaw key
        if (item.getItemMeta().getPersistentDataContainer().has(yawKey, PersistentDataType.FLOAT)) {
            float yaw = item.getItemMeta().getPersistentDataContainer().get(yawKey, PersistentDataType.FLOAT);
            crystalTeleportLocation.setYaw(yaw);
        }

        player.teleportAsync(crystalTeleportLocation);

        // Sound effect
        if (!teleportSoundString.isEmpty()) {
            player.playSound(playerLocation, teleportSound, 1, 1);
        }

        // Debug
        tpCrystalsLoader.getSLF4JLogger().info("Player %s teleported to (%s) %s %s %s using a teleportation crystal".formatted(player.getName(), savedWorld, savedLocation[0], savedLocation[1], savedLocation[2]));

        // Check if the player should be told they teleported
        if (!crystalTeleportMessage.isEmpty()) {
            player.sendMessage(miniMsg.deserialize(crystalTeleportMessage));
        }
    }
}
