package com.packetbyte.loadouts.modules;

import com.packetbyte.loadouts.Loadouts;
import com.packetbyte.loadouts.data.ItemMatcher;
import com.packetbyte.loadouts.data.Loadout;
import com.packetbyte.loadouts.data.LoadoutManager;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class LoadoutModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> activeLoadout = sgGeneral.add(new StringSetting.Builder()
        .name("active-loadout")
        .description("The loadout used by the apply keybind.")
        .defaultValue("")
        .build()
    );

    private final Setting<NotifyMode> notifyMode = sgGeneral.add(new EnumSetting.Builder<NotifyMode>()
        .name("notify-mode")
        .description("How to notify you about missing items.")
        .defaultValue(NotifyMode.Both)
        .build()
    );

    private final Setting<Boolean> reportSubstitutions = sgGeneral.add(new BoolSetting.Builder()
        .name("report-substitutions")
        .description("Mention when a similar item was used instead of the exact one.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> applyBind = sgGeneral.add(new KeybindSetting.Builder()
        .name("apply-bind")
        .description("Applies the active loadout when pressed.")
        .defaultValue(Keybind.none())
        .build()
    );

    private boolean applying = false;
    private int applyTicks = 0;
    private Map<Integer, Item> targets = new LinkedHashMap<>();
    private String applyingName = "";
    private boolean bindWasPressed = false;

    public LoadoutModule() {
        super(Loadouts.CATEGORY, "loadouts", "Saves inventory layouts and sorts your items into place.");
    }

    @Override
    public void onActivate() {
        resetApply();
    }

    @Override
    public void onDeactivate() {
        resetApply();
    }

    public void requestApply(String name) {
        if (applying) {
            error("Already applying %s, wait for it to finish.", applyingName);
            return;
        }

        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) return;

        Loadout loadout = LoadoutManager.get(name);
        if (loadout == null) {
            error("No loadout named %s. Use (highlight).loadout save <name>(default) first.", name);
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
            warning("Skipped unknown items: %s", String.join(", ", unknown));
        }

        if (found.isEmpty()) {
            error("Loadout %s is empty.", name);
            return;
        }

        targets = found;
        applyingName = loadout.name;
        applyTicks = 0;
        applying = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean pressed = applyBind.get().isPressed();
        if (pressed && !bindWasPressed && isActive() && !applying && !activeLoadout.get().isBlank()) {
            requestApply(activeLoadout.get());
        }
        bindWasPressed = pressed;

        if (!applying) return;

        if (mc.player == null || mc.gameMode == null || mc.getConnection() == null) {
            resetApply();
            return;
        }

        AbstractContainerMenu menu = mc.player.containerMenu;

        if (menu != mc.player.inventoryMenu) {
            abort("Close any open container before applying a loadout.");
            return;
        }

        applyTicks++;
        if (applyTicks > 200) {
            abort("Applying %s took too long.", applyingName);
            return;
        }

        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            int free = firstFreeLogicalSlot(menu);
            if (free != -1) click(menu, free, ContainerInput.PICKUP);
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

    private void finish() {
        NotifyMode mode = notifyMode.get();

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

        if (mode.showChat()) {
            if (missing.isEmpty()) {
                info("Loadout (highlight)%s(default) applied%s.", applyingName,
                    !substituted.isEmpty() ? " with substitutions" : "");
            } else {
                warning("Missing: %s", String.join(", ", missing));
            }
        }

        if (mode.showToast()) {
            if (missing.isEmpty()) {
                Item chest = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("chest"));
                mc.gui.toastManager().addToast(new MeteorToast.Builder("Loadouts")
                    .text("Applied " + applyingName + " (" + satisfied + "/" + targets.size() + ")").icon(chest).build());
            } else {
                mc.gui.toastManager().addToast(new MeteorToast.Builder("Loadouts")
                    .text("Missing: " + String.join(", ", missing)).build());
            }
        }

        if (reportSubstitutions.get() && mode.showChat() && !substituted.isEmpty()) {
            info("Used similar items for: %s", String.join(", ", substituted));
        }

        resetApply();
    }

    private void addMissing(List<String> list, Item item) {
        ItemStack display = new ItemStack(item);
        String name = display.getHoverName().getString();
        for (String entry : list) {
            if (entry.endsWith(name)) {
                list.set(list.indexOf(entry), bumpCount(entry, name));
                return;
            }
        }
        list.add("1x " + name);
    }

    private String bumpCount(String entry, String name) {
        String countPart = entry.substring(0, entry.length() - name.length()).replace("x ", "").trim();
        int count;
        try {
            count = Integer.parseInt(countPart);
        } catch (NumberFormatException e) {
            count = 1;
        }
        return (count + 1) + "x " + name;
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
        int srcId = Loadout.menuSlot(sourceSlot);
        int dstId = Loadout.menuSlot(destSlot);

        if (menu.getSlot(dstId).getItem().isEmpty()) {
            click(menu, srcId, ContainerInput.PICKUP);
            click(menu, dstId, ContainerInput.PICKUP);
        } else {
            click(menu, dstId, ContainerInput.PICKUP);
            click(menu, srcId, ContainerInput.PICKUP);
            click(menu, dstId, ContainerInput.PICKUP);
        }
    }

    private void click(AbstractContainerMenu menu, int logicalSlot, ContainerInput input) {
        int menuSlot = Loadout.menuSlot(logicalSlot);
        mc.gameMode.handleContainerInput(menu.containerId, menuSlot, 0, input, mc.player);
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

    private void abort(String message, Object... args) {
        error(message, args);
        resetApply();
    }

    private void resetApply() {
        applying = false;
        applyTicks = 0;
        targets = new LinkedHashMap<>();
        applyingName = "";
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        resetApply();
    }

    public String getActiveName() {
        return activeLoadout.get();
    }

    public void setActiveName(String name) {
        activeLoadout.set(name);
        Systems.save();
    }

    public enum NotifyMode {
        Chat, Toast, Both, Off;

        public boolean showChat() {
            return this == Chat || this == Both;
        }

        public boolean showToast() {
            return this == Toast || this == Both;
        }
    }
}
