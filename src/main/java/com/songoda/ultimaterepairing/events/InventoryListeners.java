package com.songoda.ultimaterepairing.events;

import com.songoda.ultimaterepairing.UltimateRepairing;
import com.songoda.ultimaterepairing.anvil.PlayerAnvilData.RepairType;
import com.songoda.ultimaterepairing.utils.Debugger;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

/**
 * Created by songoda on 2/25/2017.
 */
public class InventoryListeners implements Listener {

    private final UltimateRepairing instance;

    public InventoryListeners(UltimateRepairing instance) {
        this.instance = instance;
    }

    @EventHandler
    public void OnPickup(InventoryPickupItemEvent event) {
        if (event.getItem().hasMetadata("UltimateRepairing"))
            event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        try {
            Player p = (Player) event.getWhoClicked();
            if (instance.getEditor().isEditing(p)) {
                event.setCancelled(true);

                if (event.getSlot() == 11) {
                    instance.getEditor().toggleHologram(p);
                } else if (event.getSlot() == 13) {
                    instance.getEditor().toggleInfinity(p);
                } else if (event.getSlot() == 15) {
                    instance.getEditor().toggleParticles(p);
                }
            } else if (instance.getRepairHandler().getDataFor(p).getInRepair()) {
                event.setCancelled(true);

                if (event.getSlot() == 11) {
                    instance.getRepairHandler().finish(true, p);
                    p.closeInventory();
                } else if (event.getSlot() == 15) {
                    instance.getRepairHandler().finish(false, p);
                    p.closeInventory();
                }
            } else if (event.getInventory().getTitle().equals(instance.getLocale().getMessage("interface.repair.title"))) {
                event.setCancelled(true);
                Location loc = instance.getRepairHandler().getDataFor(p).getLocation();
                if (event.getSlot() == 11) {
                    p.closeInventory();
                    if (p.hasPermission("ultimaterepairing.use.ECO"))
                        instance.getRepairHandler().preRepair(p, RepairType.ECONOMY, loc);
                } else if (event.getSlot() == 13) {
                    p.closeInventory();
                    if (p.hasPermission("ultimaterepairing.use.ITEM"))
                        instance.getRepairHandler().preRepair(p, RepairType.ITEM, loc);
                } else if (event.getSlot() == 15) {
                    p.closeInventory();
                    if (p.hasPermission("ultimaterepairing.use.XP"))
                        instance.getRepairHandler().preRepair(p, RepairType.XP, loc);
                }
            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }
}