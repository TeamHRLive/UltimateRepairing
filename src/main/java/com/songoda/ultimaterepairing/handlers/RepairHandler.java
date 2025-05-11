package com.songoda.ultimaterepairing.handlers;

import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.utils.PlayerUtils;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.third_party.com.cryptomorin.xseries.XSound;
import com.songoda.ultimaterepairing.UltimateRepairing;
import com.songoda.ultimaterepairing.anvil.PlayerAnvilData;
import com.songoda.ultimaterepairing.gui.RepairGui;
import com.songoda.ultimaterepairing.gui.StartConfirmGui;
import com.songoda.ultimaterepairing.repair.RepairType;
import com.songoda.ultimaterepairing.utils.Methods;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by songoda on 2/25/2017.
 */
public class RepairHandler {

    private final UltimateRepairing plugin;
    private final GuiManager guiManager;

    private final Map<UUID, PlayerAnvilData> playerAnvilData = new HashMap<>();

    public RepairHandler(UltimateRepairing plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    private void repairType(Player p, Location l) {
        if (getDataFor(p).getInRepair()) {
            yesNo(p, getDataFor(p).getType(), getDataFor(p).getToBeRepaired());
        } else {
            RepairGui.newGui(p, l);
        }
    }


    public void preRepair(ItemStack itemStack, int playerslot, Player player, RepairType type, Location anvil) {
        // Get from Map, put new instance in Map if it doesn't exist
        PlayerAnvilData playerData = playerAnvilData.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerAnvilData());

        // Store the original slot and stack size before removing the item
        ItemStack slotContents = player.getInventory().getItem(playerslot);
        int originalAmount = 1;
        if (slotContents != null && slotContents.getAmount() > 1) {
            originalAmount = slotContents.getAmount();
            // Just reduce the amount by 1 instead of removing the whole stack
            slotContents.setAmount(originalAmount - 1);
            player.getInventory().setItem(playerslot, slotContents);
        } else {
            // If there's only one item, remove it as before
            player.getInventory().setItem(playerslot, null);
        }

        playerData.setSlot(playerslot);
        // Store the original amount to know if we need to add to the stack later
        playerData.setOriginalAmount(originalAmount);

        Item item = player.getWorld().dropItem(anvil.add(0.5, 2, 0.5), itemStack);

        // Support for EpicHoppers suction.
        item.setMetadata("grabbed", new FixedMetadataValue(plugin, "true"));

        item.setMetadata("betterdrops_ignore", new FixedMetadataValue(plugin, true));
        Vector vec = player.getEyeLocation().getDirection();
        vec.setX(0);
        vec.setY(0);
        vec.setZ(0);
        item.setVelocity(vec);
        item.setPickupDelay(3600);
        item.setMetadata("UltimateRepairing", new FixedMetadataValue(plugin, ""));

        playerData.setItem(item);
        playerData.setToBeRepaired(itemStack);
        playerData.setLocations(anvil.add(0, -2, 0));

        yesNo(player, type, itemStack);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (item.isValid() && !playerData.isBeingRepaired()) {

                plugin.getLocale().getMessage("event.repair.timeout").sendPrefixedMessage(player);
                removeItem(playerData, player);
                player.closeInventory();

            }
        }, plugin.getConfig().getLong("Main.Time Before Repair Auto Canceled"));
    }

    public void initRepair(Player player, Location anvil) {
        if (anvil.add(0, 1, 0).getBlock().getType() != Material.AIR) {
            plugin.getLocale().getMessage("event.repair.needspace").sendPrefixedMessage(player);
            return;
        }

        repairType(player, anvil);
    }

    private void yesNo(Player p, RepairType type, ItemStack item) {
        PlayerAnvilData playerData = getDataFor(p);

        if (playerData.isBeingRepaired()) {
            return;
        }

        int price = Methods.getCost(type, item);
        playerData.setInRepair(true);
        playerData.setType(type);
        playerData.setPrice(price);

        guiManager.showGUI(p, new StartConfirmGui(type, p, item));
    }


    public void finish(boolean answer, Player player) {
        PlayerAnvilData playerData = playerAnvilData.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerAnvilData());
        if (!answer) {
            removeItem(playerData, player);
            plugin.getLocale().getMessage("event.repair.cancelled").sendPrefixedMessage(player);
            return;
        }
        RepairType type = playerData.getType();
        ItemStack players = playerData.getToBeRepaired();

        boolean sold = false;
        if (type == RepairType.ECONOMY && EconomyManager.isEnabled()) {
            int price = playerData.getPrice();

            if (EconomyManager.hasBalance(player, price)) {
                EconomyManager.withdrawBalance(player, price);
                sold = true;
            }
        }

        int cost = Methods.getCost(type, players);
        ItemStack item2 = new ItemStack(Methods.getType(players), cost);
        String name = (item2.getType().name().substring(0, 1).toUpperCase() + item2.getType().name().toLowerCase().substring(1)).replace("_", " ");
        if (type == RepairType.ITEM && Methods.inventoryContains(player.getInventory(), item2)) {
            Methods.removeFromInventory(player.getInventory(), item2);
            sold = true;
        }

        if (type == RepairType.EXPERIENCE && player.getLevel() >= playerData.getPrice() || sold || player.getGameMode() == GameMode.CREATIVE) {
            playerData.setBeingRepaired(true);

            Effect effect = Effect.STEP_SOUND;

            XMaterial blockType = XMaterial.REDSTONE_BLOCK;

            String typeStr = playerData.getToBeRepaired().getType().name();

            if (typeStr.contains("NETHERITE")) {
                blockType = XMaterial.NETHERITE_BLOCK;
            } else if (typeStr.contains("DIAMOND")) {
                blockType = XMaterial.DIAMOND_BLOCK;
            } else if (typeStr.contains("IRON")) {
                blockType = XMaterial.IRON_BLOCK;
            } else if (typeStr.contains("GOLD")) {
                blockType = XMaterial.GOLD_BLOCK;
            } else if (typeStr.contains("STONE")) {
                blockType = XMaterial.STONE;
            } else if (typeStr.contains("WOOD")) {
                blockType = XMaterial.OAK_WOOD;
            }

            final Material blockTypeFinal = blockType.parseMaterial();

            Location location = playerData.getLocations();
            player.getWorld().playEffect(location, effect, blockTypeFinal);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Runnable runnable = () -> player.getWorld().playEffect(location, effect, blockTypeFinal);

                    // Delay for 5 ticks (0.25 seconds)
                    Thread.sleep(250);
                    Bukkit.getScheduler().runTask(plugin, runnable);

                    // Delay for 10 ticks (0.5 seconds)
                    Thread.sleep(500);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.getWorld().playEffect(location, effect, blockTypeFinal);
                        player.getWorld().playEffect(location, effect, Material.STONE);
                        XSound.BLOCK_ANVIL_LAND.play(player);
                    });

                    // Delay for 15 ticks (0.75 seconds)
                    Thread.sleep(500);
                    Bukkit.getScheduler().runTask(plugin, runnable);

                    // Delay for 20 ticks (1 second)
                    Thread.sleep(250);
                    Bukkit.getScheduler().runTask(plugin, runnable);

                    // Delay for 20 ticks (1 second)
                    Thread.sleep(250);
                    Bukkit.getScheduler().runTask(plugin, runnable);

                    // Delay for 25 ticks (1.25 seconds)
                    Thread.sleep(500);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        XSound.BLOCK_ANVIL_LAND.play(player);
                        player.getWorld().playEffect(location, effect, blockTypeFinal);
                        player.getWorld().playEffect(location, effect, Material.ANVIL);

                        // Move the repaired item up
                        Item item = playerData.getItem();
                        if (item != null && item.isValid()) {
                            item.setVelocity(new Vector(0, 0.3, 0)); // Adjust the velocity as needed
                            item.setPickupDelay(20); // Adjust the pickup delay as needed
                        }
                    });
                    Thread.sleep(250);
                    XSound.ENTITY_PLAYER_LEVELUP.play(player);

                    // Delay for 30 ticks (1.5 seconds)
                    Thread.sleep(300);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getLocale().getMessage("event.repair.success").sendPrefixedMessage(player);
                        playerData.getToBeRepaired().setDurability((short) 0);
                        removeItem(playerData, player);
                        if (player.getGameMode() != GameMode.CREATIVE && type == RepairType.EXPERIENCE) {
                            player.setLevel(player.getLevel() - playerData.getPrice());
                        }
                        player.closeInventory();
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            return;
        }

        if (type == RepairType.ECONOMY) {
            plugin.getLocale().getMessage("event.repair.notenough")
                    .processPlaceholder("type", plugin.getLocale().getMessage("interface.repair.eco").toText())
                    .sendPrefixedMessage(player);
        } else if (type == RepairType.EXPERIENCE)
            plugin.getLocale().getMessage("event.repair.notenough")
                    .processPlaceholder("type", plugin.getLocale().getMessage("interface.repair.xp").toText())
                    .sendPrefixedMessage(player);
        else
            plugin.getLocale().getMessage("event.repair.notenough")
                    .processPlaceholder("type", name).sendPrefixedMessage(player);

        // we've failed to repair, so return the item
        removeItem(playerData, player);
    }

    public void removeItem(PlayerAnvilData playerData, Player player) {
        int slot = playerData.getSlot();

        if (slot < 0) {
            // If no valid slot, just give the item
            PlayerUtils.giveItem(player, playerData.getToBeRepaired());
        } else {
            // Check if the original slot already has items
            ItemStack currentItem = player.getInventory().getItem(slot);

            if (currentItem != null && currentItem.isSimilar(playerData.getToBeRepaired())) {
                // There are still items of the same type in that slot, just increase the amount
                currentItem.setAmount(currentItem.getAmount() + 1);
            } else if (currentItem == null) {
                // The slot is empty, put the repaired item there
                player.getInventory().setItem(slot, playerData.getToBeRepaired());
            } else {
                // The slot now has a different item, just give the player the repaired item
                PlayerUtils.giveItem(player, playerData.getToBeRepaired());
            }
        }

        if (playerData.getItem() != null) {
            playerData.getItem().remove();
        }

        this.playerAnvilData.remove(player.getUniqueId());
    }

    public boolean hasInstance(Player player) {
        return playerAnvilData.containsKey(player.getUniqueId());
    }

    public PlayerAnvilData getDataFor(Player player) {
        return playerAnvilData.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerAnvilData());
    }

}