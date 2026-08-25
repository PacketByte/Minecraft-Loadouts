package com.packetbyte.loadouts.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ItemMatcher {
    private static final List<String> PREFIXES = new ArrayList<>(Arrays.asList(
        "pale_oak", "dark_oak", "light_gray", "light_blue", "bamboo_mosaic",
        "oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry", "bamboo", "crimson", "warped",
        "netherite", "diamond", "golden", "copper", "iron", "stone", "wooden", "leather", "chainmail",
        "smooth", "polished", "mossy", "cracked", "chiseled", "deepslate",
        "white", "orange", "magenta", "cyan", "yellow", "lime", "pink",
        "gray", "purple", "blue", "brown", "green", "red", "black"
    ));

    static {
        PREFIXES.sort(Comparator.comparingInt(String::length).reversed());
    }

    private ItemMatcher() {}

    public static boolean matches(ItemStack stack, Item required) {
        if (stack.isEmpty() || required == null) return false;
        if (stack.getItem() == required) return true;

        String a = groupOf(required);
        return !a.isBlank() && a.equals(groupOf(stack.getItem()));
    }

    public static String groupOf(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();

        for (int pass = 0; pass < 2; pass++) {
            boolean stripped = false;
            for (String prefix : PREFIXES) {
                if (path.startsWith(prefix + "_") && path.length() > prefix.length() + 1) {
                    path = path.substring(prefix.length() + 1);
                    stripped = true;
                    break;
                }
            }
            if (!stripped) break;
        }

        return path;
    }
}
