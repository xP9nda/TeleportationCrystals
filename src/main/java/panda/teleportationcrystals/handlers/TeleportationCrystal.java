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
        this.crystalUUIDKey = new NamespacedKey(loader, "uuid");
        this.crystalCraftingRecipeKey = new NamespacedKey(loader, "panda.teleportationcrystalrecipe");

        reloadTeleportationCrystalConfig();
        updateTeleportationCrystalItemStack(crystalDefaultUses);
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
        crystalDefaultUses = config.getInt("crystal_default_uses");
        crystalRecipeStrings = config.getStringList("crystal_recipe");
        teleportationCrystalCraftingEnabled = config.getBoolean("crystal_recipe_enabled");

        // Check if a recipe is already set when the crafting is disabled
        if (teleportationCrystalCraftingEnabled == false && teleportationCrystalRecipe != null) {
            // Remove the recipe if it exists
            tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
        }

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
    private void updateTeleportationCrystalItemStack(int uses) {
        Integer finalUses = uses;

        this.teleportationCrystalItem = new ItemStack(crystalMaterial);

        this.teleportationCrystalItem.editMeta(itemMeta -> {
            itemMeta.displayName(miniMsg.deserialize(crystalItemName).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

            itemMeta.getPersistentDataContainer().set(this.locationKey, PersistentDataType.INTEGER_ARRAY, new int[] { 0, 0, 0 });
            itemMeta.getPersistentDataContainer().set(this.worldKey, PersistentDataType.STRING, "");
            itemMeta.getPersistentDataContainer().set(this.itemKey, PersistentDataType.STRING, "panda.teleportation_crystal");

            itemMeta.getPersistentDataContainer().set(this.usesKey, PersistentDataType.INTEGER, finalUses);
            itemMeta.getPersistentDataContainer().set(this.crystalUUIDKey, PersistentDataType.STRING, UUID.randomUUID().toString());

            itemMeta.lore(
                    locationUnsetLoreStrings.stream().map(
                            it -> miniMsg.deserialize(
                                    it,
                                    Placeholder.unparsed("uses", String.valueOf(finalUses))
                            ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    ).toList()
            );
        });

        // Setup crafting recipe
        if (teleportationCrystalCraftingEnabled == true) {
            teleportationCrystalRecipe = new ShapedRecipe(this.crystalCraftingRecipeKey, this.teleportationCrystalItem);
            teleportationCrystalRecipe.shape("012", "345", "678");

            boolean validRecipe = true;
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
                    validRecipe = false;
                    return;
                }

                String indexString = Integer.toString(i);
                Character recipeCharacterIdentifier = indexString.charAt(0);
                teleportationCrystalRecipe.setIngredient(recipeCharacterIdentifier, currentRecipeItemMaterial);
            }

            // Check if the recipe is valid and if it is, add it as a recipe
            if (!validRecipe) {
                return;
            }

            tpCrystalsLoader.getServer().addRecipe(teleportationCrystalRecipe);
        }
    }

    // Command for giving players teleportation crystals
    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal give <player> [uses]")
    @CommandPermission("panda.teleportationcrystals.give")
    private void onTeleportationCrystalGive(Player commandSender, @Argument("player") Player player , @Argument("uses") @Range(min = "1", max = "5000") Integer uses) {
        // Check if the uses is defined
        if (uses == null) {
            uses = crystalDefaultUses;
        }

        // Add the crystal to the player's inventory
        player.getInventory().addItem(this.teleportationCrystalItem);

        // Remove the old crafting recipe
        if (teleportationCrystalCraftingEnabled == true) {
            tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
        }
        // Update the itemstack and recipe
        updateTeleportationCrystalItemStack(uses);


        // Check if the receiver should be sent a message
        if (!receivedCrystalMessage.isEmpty()) {
            player.sendMessage(miniMsg.deserialize(
                    receivedCrystalMessage,
                    Placeholder.unparsed("player", commandSender.getName()),
                    Placeholder.unparsed("uses", String.valueOf(uses))
            ));
        }

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
        updateTeleportationCrystalItemStack(crystalDefaultUses);

        // Check if the reload message should be sent
        if (!reloadMessage.isEmpty()) {
            commandSender.sendMessage(miniMsg.deserialize(reloadMessage));
        }
    }

    // Command for plugin info
    @CommandMethod("teleportationcrystal|teleportcrystal|tpcrystal info|information")
    private void onTeleportationCrystalInformation(Player commandSender) {
        commandSender.sendMessage(
            miniMsg.deserialize(
            "<#89C9B8>TeleportationCrystals<#75A1BF> by xP9nda<br><#89C9B8>Source code available on<#75A1BF> <click:open_url:'https://github.com/xP9nda/TeleportationCrystals'><u>GitHub</u></click>"
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
        if (craftingResult == null) {
            return;
        }

        // Check if the result is the same as the item stack
        if (!craftingResult.isSimilar(teleportationCrystalRecipe.getResult())) {
            return;
        }

        // Remove the old crafting recipe
        if (teleportationCrystalCraftingEnabled == true) {
            tpCrystalsLoader.getServer().removeRecipe(this.crystalCraftingRecipeKey);
        }
        // Update the itemstack and recipe
        updateTeleportationCrystalItemStack(crystalDefaultUses);
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
        if (savedWorld.isEmpty() && !crystalNoPositionMessage.isEmpty()) {
            // No saved location
            player.sendMessage(miniMsg.deserialize(crystalNoPositionMessage));

            // Sound effect
            if (!teleportSoundFailString.isEmpty()) {
                player.playSound(playerLocation, teleportSoundFail, 1, 1);
            }

            return;
        }

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
                                    Placeholder.unparsed("uses", String.valueOf(finalRemainingUses))
                            ).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    ).toList()
            );
        });

        // Saved location exists, get the location and teleport the player
        int[] savedLocation = item.getItemMeta().getPersistentDataContainer().get(locationKey, PersistentDataType.INTEGER_ARRAY);
        Location crystalTeleportLocation = new Location(Bukkit.getWorld(savedWorld), savedLocation[0], savedLocation[1], savedLocation[2]);

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
