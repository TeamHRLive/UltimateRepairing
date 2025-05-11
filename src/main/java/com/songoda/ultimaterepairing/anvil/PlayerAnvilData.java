package com.songoda.ultimaterepairing.anvil;

import com.songoda.ultimaterepairing.repair.RepairType;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class PlayerAnvilData {

    private Location location;
    private int price;
    private RepairType type;
    private Item item;
    private ItemStack toBeRepaired;
    private int slot = -1;
    private Location locations;
    private boolean inRepair;
    private boolean beingRepaired;
    private int originalAmount = 1;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public RepairType getType() {
        return type;
    }

    public void setType(RepairType type) {
        this.type = type;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemStack getToBeRepaired() {
        return toBeRepaired;
    }

    public void setToBeRepaired(ItemStack toBeRepaired) {
        this.toBeRepaired = toBeRepaired;
    }

    public Location getLocations() {
        return locations;
    }

    public void setLocations(Location locations) {
        this.locations = locations;
    }

    public boolean getInRepair() {
        return inRepair;
    }

    public void setInRepair(boolean inRepair) {
        this.inRepair = inRepair;
    }

    public boolean isBeingRepaired() {
        return beingRepaired;
    }

    public void setBeingRepaired(boolean beingRepaired) {
        this.beingRepaired = beingRepaired;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public int getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(int originalAmount) {
        this.originalAmount = originalAmount;
    }
}