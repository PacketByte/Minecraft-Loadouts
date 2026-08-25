package com.packetbyte.loadouts.engine;

import com.packetbyte.loadouts.Loadouts;
import com.packetbyte.loadouts.data.ItemMatcher;
import com.packetbyte.loadouts.data.Loadout;
import com.packetbyte.loadouts.data.LoadoutManager;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ApplyEngine {
    private static final ApplyEngine INSTANCE = new ApplyEngine();

    private boolean subscribed;
    private boolean applying;
    private int ticks;
    private String name = "";
    private Map<Integer, Item> targets = new LinkedHashMap<>();

    public static ApplyEngine get() {
        return INSTANCE;
    }

    private ApplyEngine() {}

    public boolean isApplying() {
        return applying;
    }

    public String applyingName() {
        return name;
    }

    public void start(String loadoutName) {
        if (applying) {
            chat("Already sorting " + name + ", hang on.");
            return;
        }

        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) return;

        Loadout loadout = LoadoutManager.get(loadoutName);
        if (loadout == null) {
            chat("No loadout called " + loadoutName + ".");
            return;
        }

        Map<Integer, Item> found = new LinkedHashMap<>();
        List<String> unknown = new ArrayList<>();

        for (int slot : loadout.filledSlots()) {
            Item item = Loadout.itemFromId(loadout.get(slot));
            if (item != null) found.put(slot, item);
            else unknown.add(loadout.get(slot));
        }

        if (!unknown.isEmpty()) {
            chat("Skipped items not in this game version: " + String.join(", ", unknown));
        }

        if (found.isEmpty()) {
            chat("Loadout " + loadout.name + " is empty.");
            return;
        }

        targets = found;
        name = loadout.name;
        ticks = 0;
        applying = true;

        if (!subscribed) {
            MeteorClient.EVENT_BUS.subscribe(this);
            subscribed = true;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!applying) return;

        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            stop();
            return;
        }

        AbstractContainerMenu menu = mc.player.containerMenu;

        if (menu != mc.player.inventoryMenu) {
            fail("Close any open container first, then try again.");
            return;
        }

        ticks++;
        if (ticks > 400) {
            fail("Sorting " + name + " took too long, gave up.");
            return;
        }

        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            int free = firstFreeLogicalSlot(menu);
            if (free != -1) click(menu, free, ContainerInput.PICKUP);
            else fail("No free space to put down a held item.");
            return;
        }

        for (Map.Entry<Integer, Item> target : targets.entrySet()) {
            int slot = target.getKey();
            Item required = target.getValue();

            if (ItemMatcher.matches(menu.getSlot(Loadout.menuSlot(slot)).getItem(), required)) continue;

            int source = findSource(targets, slot, required);
            if (source == -1) continue;

            executeMove(menu, source, slot);
            return;
        }

        finish();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        stop();
    }

    private void finish() {
        List<String> missing = new ArrayList<>();
        List<String> substituted = new ArrayList<>();
        int satisfied = 0;

        for (Map.Entry<Integer, Item> target : targets.entrySet()) {
            ItemStack stack = mc.player.containerMenu.getSlot(Loadout.menuSlot(target.getKey())).getItem();
            Item required = target.getValue();

            if (stack.isEmpty()) {
                addMissing(missing, required);
            } else if (stack.getItem() == required) {
                satisfied++;
            } else if (ItemMatcher.matches(stack, required)) {
                satisfied++;
                substituted.add(ItemMatcher.groupOf(required));
            } else {
                addMissing(missing, required);
            }
        }

        if (missing.isEmpty()) {
            chat("Applied " + name + " (" + satisfied + "/" + targets.size() + ")"
                + (!substituted.isEmpty() ? " with similar items" : "") + ".");
            toast("Applied " + name + " (" + satisfied + "/" + targets.size() + ")");
        } else {
            chat("Missing: " + String.join(", ", missing));
            toast("Missing: " + String.join(", ", missing));
        }

        stop();
    }

    private void fail(String message) {
        chat(message);
        toast("Sort failed");
        stop();
    }

    private void stop() {
        applying = false;
        ticks = 0;
        targets = new LinkedHashMap<>();
        name = "";

        if (subscribed) {
            MeteorClient.EVENT_BUS.unsubscribe(this);
            subscribed = false;
        }
    }

    private int findSource(Map<Integer, Item> allTargets, int excludeSlot, Item required) {
        int best = -1;
        int bestScore = -1;

        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            if (slot == excludeSlot) continue;
            if (isLockedSatisfied(allTargets, slot)) continue;

            ItemStack stack = mc.player.containerMenu.getSlot(Loadout.menuSlot(slot)).getItem();
            if (stack.isEmpty() || !ItemMatcher.matches(stack, required)) continue;

            int score = stack.getItem() == required ? 100 : 10;
            if (!allTargets.containsKey(slot)) score += 5;

            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }

        return best;
    }

    private boolean isLockedSatisfied(Map<Integer, Item> allTargets, int slot) {
        Item ownTarget = allTargets.get(slot);
        if (ownTarget == null) return false;
        return ItemMatcher.matches(mc.player.containerMenu.getSlot(Loadout.menuSlot(slot)).getItem(), ownTarget);
    }

    private void executeMove(AbstractContainerMenu menu, int sourceSlot, int destSlot) {
        if (menu.getSlot(Loadout.menuSlot(destSlot)).getItem().isEmpty()) {
            click(menu, sourceSlot, ContainerInput.PICKUP);
            click(menu, destSlot, ContainerInput.PICKUP);
        } else {
            click(menu, destSlot, ContainerInput.PICKUP);
            click(menu, sourceSlot, ContainerInput.PICKUP);
            click(menu, destSlot, ContainerInput.PICKUP);
        }
    }

    private void click(AbstractContainerMenu menu, int logicalSlot, ContainerInput input) {
        mc.gameMode.handleContainerInput(menu.containerId, Loadout.menuSlot(logicalSlot), 0, input, mc.player);
    }

    private int firstFreeLogicalSlot(AbstractContainerMenu menu) {
        for (int slot = 35; slot >= 9; slot--) {
            if (menu.getSlot(Loadout.menuSlot(slot)).getItem().isEmpty()
                && !targets.containsKey(slot)) return slot;
        }
        for (int slot = 8; slot >= 0; slot--) {
            if (menu.getSlot(Loadout.menuSlot(slot)).getItem().isEmpty()
                && !targets.containsKey(slot)) return slot;
        }
        return -1;
    }

    private void addMissing(List<String> list, Item item) {
        ItemStack display = new ItemStack(item);
        String label = display.getHoverName().getString();
        for (String entry : list) {
            if (entry.endsWith(label)) {
                list.set(list.indexOf(entry), bumpCount(entry, label));
                return;
            }
        }
        list.add("1x " + label);
    }

    private String bumpCount(String entry, String label) {
        String countPart = entry.substring(0, entry.length() - label.length()).replace("x ", "").trim();
        int count;
        try {
            count = Integer.parseInt(countPart);
        } catch (NumberFormatException e) {
            count = 1;
        }
        return (count + 1) + "x " + label;
    }

    private static void chat(String message) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("[Loadouts] " + message));
        }
    }

    private static void toast(String text) {
        if (mc.gui == null || mc.gui.toastManager() == null) return;
        Item chest = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("chest"));
        MeteorToast.Builder builder = new MeteorToast.Builder("Loadouts").text(text).duration(5000);
        if (chest != null) builder.icon(chest);
        mc.gui.toastManager().addToast(builder.build());
    }
}
