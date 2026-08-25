package com.packetbyte.loadouts.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoadoutManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FOLDER = new File(MeteorClient.FOLDER, "loadouts");

    private static final Map<String, Loadout> loadouts = new LinkedHashMap<>();

    private LoadoutManager() {}

    public static void init() {
        if (!FOLDER.exists() && !FOLDER.mkdirs()) {
            System.err.println("Failed to create the Loadouts folder.");
            return;
        }

        File[] files = FOLDER.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                Loadout loadout = GSON.fromJson(json, Loadout.class);
                if (loadout != null && loadout.name != null && !loadout.name.isBlank()) {
                    loadouts.put(keyOf(loadout.name), loadout);
                }
            } catch (Exception e) {
                System.err.println("Failed to read loadout " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public static void saveAll() {
        for (Loadout loadout : loadouts.values()) saveToDisk(loadout);
    }

    private static void saveToDisk(Loadout loadout) {
        try {
            if (!FOLDER.exists()) FOLDER.mkdirs();
            File file = new File(FOLDER, fileName(loadout.name));
            Files.writeString(file.toPath(), GSON.toJson(loadout), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save loadout " + loadout.name + ": " + e.getMessage());
        }
    }

    public static boolean save(Loadout loadout) {
        loadouts.put(keyOf(loadout.name), loadout);
        saveToDisk(loadout);
        return true;
    }

    public static boolean delete(String name) {
        String key = keyOf(name);
        if (!loadouts.containsKey(key)) return false;

        loadouts.remove(key);

        File file = new File(FOLDER, fileName(name));
        if (file.exists()) file.delete();

        return true;
    }

    public static boolean rename(String oldName, String newName) {
        Loadout loadout = get(oldName);
        if (loadout == null || exists(newName)) return false;

        delete(oldName);
        loadout.name = newName;
        return save(loadout);
    }

    public static Loadout get(String name) {
        return loadouts.get(keyOf(name));
    }

    public static boolean exists(String name) {
        return loadouts.containsKey(keyOf(name));
    }

    public static List<Loadout> all() {
        List<Loadout> list = new ArrayList<>(loadouts.values());
        list.sort(Comparator.comparing(l -> l.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    public static int count() {
        return loadouts.size();
    }

    public static Loadout capture(Player player, String name) {
        Loadout loadout = new Loadout(sanitizeName(name));

        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            ItemStack stack = Loadout.stackAt(player, slot);
            if (!stack.isEmpty()) {
                loadout.set(slot, Loadout.idFromItem(stack.getItem()));
            }
        }

        return loadout;
    }

    public static String sanitizeName(String raw) {
        String cleaned = raw.trim().replaceAll("[^a-zA-Z0-9 _.-]", "_");
        if (cleaned.isBlank()) cleaned = "unnamed";
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    private static String keyOf(String name) {
        return name.toLowerCase(Locale.ROOT).trim();
    }

    private static String fileName(String name) {
        return keyOf(name).replaceAll("[^a-z0-9_.-]", "_") + ".json";
    }
}
