package com.packetbyte.loadouts.data;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class Loadout {
    public static final int SIZE = 41;
    public static final int ARMOR_START = 36;
    public static final int OFFHAND = 40;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public String name = "unnamed";
    public String[] items = new String[SIZE];

    public Loadout() {}

    public Loadout(String name) {
        this.name = name;
    }

    public String get(int slot) {
        if (slot < 0 || slot >= SIZE) return null;
        return items[slot];
    }

    public void set(int slot, String itemId) {
        if (slot < 0 || slot >= SIZE) return;
        items[slot] = itemId;
    }

    public void clear(int slot) {
        set(slot, null);
    }

    public void clearAll() {
        items = new String[SIZE];
    }

    public int countFilled() {
        int n = 0;
        for (String item : items) {
            if (item != null && !item.isBlank()) n++;
        }
        return n;
    }

    public List<Integer> filledSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            if (items[i] != null && !items[i].isBlank()) slots.add(i);
        }
        return slots;
    }

    public static Item itemFromId(String id) {
        try {
            Identifier loc = Identifier.tryParse(id.trim().toLowerCase());
            if (loc == null) return null;
            return BuiltInRegistries.ITEM.getValue(loc);
        } catch (Exception e) {
            return null;
        }
    }

    public static String idFromItem(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static ItemStack stackAt(Player player, int slot) {
        Inventory inv = player.getInventory();
        if (slot >= 0 && slot < 36) return inv.getNonEquipmentItems().get(slot);
        if (slot >= ARMOR_START && slot < OFFHAND) return player.getItemBySlot(ARMOR_SLOTS[slot - ARMOR_START]);
        if (slot == OFFHAND) return player.getItemBySlot(EquipmentSlot.OFFHAND);
        return ItemStack.EMPTY;
    }

    public static int menuSlot(int logical) {
        if (logical < 9) return 36 + logical;
        if (logical < 36) return logical;
        if (logical >= ARMOR_START && logical < OFFHAND) return 5 + (logical - ARMOR_START);
        if (logical == OFFHAND) return 45;
        return -1;
    }
}
