package com.packetbyte.loadouts.gui;

import com.packetbyte.loadouts.data.ItemMatcher;
import com.packetbyte.loadouts.data.Loadout;
import com.packetbyte.loadouts.data.LoadoutManager;
import com.packetbyte.loadouts.engine.ApplyEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LoadoutScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final int PANEL_W = 120;
    private static final int ROW_H = 12;

    private final Map<String, ItemStack> icons = new HashMap<>();

    private Loadout working;
    private String selectedName = "";
    private List<String> saved = new ArrayList<>();
    private int scroll = 0;

    private EditBox nameBox;
    private int panelX;
    private int listY;
    private int listRows = 1;

    private String statusMsg = "";
    private int statusTicks;

    public LoadoutScreen(String loadoutName) {
        super(player().inventoryMenu, player().getInventory(), Component.literal("Loadouts"));
        refreshSaved();
        select(loadoutName);
    }

    private static Player player() {
        return Minecraft.getInstance().player;
    }

    @Override
    protected void init() {
        super.init();

        panelX = leftPos + imageWidth + 10;
        listY = topPos + 14;

        int controlsH = 16 + 4 + 18 + 4 + 18 + 4 + 18;
        int available = Math.max(ROW_H, this.height - listY - 6 - controlsH);
        listRows = Math.max(1, available / ROW_H);

        int cy = listY + listRows * ROW_H + 6;

        nameBox = new EditBox(this.font, panelX, cy, PANEL_W, 16, Component.literal("Loadout name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(selectedName);
        addRenderableWidget(nameBox);

        int by = cy + 20;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
            .bounds(panelX, by, 58, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Capture"), b -> captureAll())
            .bounds(panelX + 62, by, 58, 18).build());

        by += 22;
        addRenderableWidget(Button.builder(Component.literal("New"), b -> newBlank())
            .bounds(panelX, by, 58, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteCurrent())
            .bounds(panelX + 62, by, 58, 18).build());

        by += 22;
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> apply())
            .bounds(panelX, by, PANEL_W, 18).build());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (statusTicks > 0) statusTicks--;
    }

    private void refreshSaved() {
        saved = new ArrayList<>();
        for (Loadout loadout : LoadoutManager.all()) saved.add(loadout.name);
        clampScroll();
    }

    private void clampScroll() {
        int maxScroll = Math.max(0, saved.size() - listRows);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;
    }

    private void select(String loadoutName) {
        Loadout stored = loadoutName == null || loadoutName.isEmpty() ? null : LoadoutManager.get(loadoutName);

        if (stored != null) {
            working = new Loadout(stored.name);
            for (int slot = 0; slot < Loadout.SIZE; slot++) working.set(slot, stored.get(slot));
            selectedName = stored.name;
        } else {
            working = new Loadout("");
            selectedName = "";
        }

        if (nameBox != null) nameBox.setValue(selectedName);
    }

    private void save() {
        String name = currentName();
        if (name == null) {
            status("Type a name first.");
            return;
        }

        working.name = name;
        LoadoutManager.save(working);
        refreshSaved();
        selectedName = name;
        status("Saved " + name + ".");
    }

    private String currentName() {
        String raw = nameBox.getValue().trim();
        if (raw.isEmpty()) return selectedName.isEmpty() ? null : selectedName;
        return LoadoutManager.sanitizeName(raw);
    }

    private void captureAll() {
        Player p = player();
        if (p == null) return;

        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            ItemStack stack = Loadout.stackAt(p, slot);
            if (stack == null || stack.isEmpty()) working.clear(slot);
            else working.set(slot, Loadout.idFromItem(stack.getItem()));
        }
        status("Copied your current inventory.");
    }

    private void newBlank() {
        working = new Loadout("");
        selectedName = "";
        nameBox.setValue("");
        status("Blank loadout. Ctrl+click slots to mark them.");
    }

    private void deleteCurrent() {
        if (selectedName.isEmpty()) {
            status("Nothing selected.");
            return;
        }

        if (LoadoutManager.delete(selectedName)) status("Deleted " + selectedName + ".");
        else status("Could not find " + selectedName + ".");

        selectedName = "";
        refreshSaved();
    }

    private void apply() {
        String name = currentName();
        if (name == null) {
            status("Save it with a name before applying.");
            return;
        }

        working.name = name;
        LoadoutManager.save(working);
        refreshSaved();
        selectedName = name;

        ApplyEngine.get().start(name);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();

        if (mx >= panelX && mx <= panelX + PANEL_W && my >= listY && my < listY + listRows * ROW_H) {
            int index = scroll + (int) ((my - listY) / ROW_H);
            if (index >= 0 && index < saved.size()) {
                select(saved.get(index));
                status("Selected " + saved.get(index) + ".");
            }
            return true;
        }

        if (event.hasControlDown() && hoveredSlot != null
            && mc.player != null && mc.player.containerMenu == mc.player.inventoryMenu
            && !ApplyEngine.get().isApplying()) {

            int logical = logicalFromMenu(hoveredSlot.index);
            if (logical != -1) {
                ItemStack held = hoveredSlot.getItem();
                if (working.get(logical) != null) {
                    working.clear(logical);
                    status("Removed from loadout.");
                } else if (!held.isEmpty()) {
                    working.set(logical, Loadout.idFromItem(held.getItem()));
                    status("Added to loadout.");
                } else {
                    status("That slot is empty.");
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelX && mouseX <= panelX + PANEL_W && mouseY >= listY && mouseY < listY + listRows * ROW_H) {
            scroll += scrollY > 0 ? -1 : 1;
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        g.text(this.font, "Loadouts", panelX, topPos + 2, 0xFFFFFFFF, true);

        int maxVisible = Math.min(saved.size() - scroll, listRows);
        for (int row = 0; row < maxVisible; row++) {
            int index = scroll + row;
            String entry = saved.get(index);
            boolean active = entry.equalsIgnoreCase(selectedName);

            int color = active ? 0xFFFFFFFF : 0xFF909090;
            String prefix = active ? "> " : "";
            g.text(this.font, prefix + entry, panelX, listY + row * ROW_H, color, true);
        }

        int controlsTop = listY + listRows * ROW_H + 6;
        int footerY = controlsTop + 86;

        if (ApplyEngine.get().isApplying()) {
            g.text(this.font, "Sorting " + ApplyEngine.get().applyingName() + "...",
                panelX, footerY, 0xFFFFFF55, true);
        } else if (statusTicks > 0 && !statusMsg.isEmpty()) {
            g.text(this.font, statusMsg, panelX, footerY, 0xFF787878, true);
        }

        renderGhosts(g, mouseX, mouseY);
    }

    private void renderGhosts(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (mc.player == null || ApplyEngine.get().isApplying()) return;

        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            String id = working.get(slot);
            if (id == null || id.isBlank()) continue;

            Item want = Loadout.itemFromId(id);
            if (want == null) continue;

            int menuId = Loadout.menuSlot(slot);
            if (menuId == -1) continue;

            Slot guiSlot = mc.player.inventoryMenu.slots.get(menuId);
            int x = leftPos + guiSlot.x;
            int y = topPos + guiSlot.y;

            ItemStack real = guiSlot.getItem();
            boolean ok = ItemMatcher.matches(real, want);

            if (!ok) g.fill(x, y, x + 16, y + 16, 0x60000000);

            g.item(icon(want), x, y);
            g.outline(x - 1, y - 1, x + 17, y + 17, ok ? 0xFF3ADB70 : 0xFFE05252);

            if (hoveredSlot == guiSlot) {
                g.setTooltipForNextFrame(this.font,
                    Component.literal((ok ? "[OK] " : "[NEED] ") + labelFor(id)), mouseX, mouseY);
            }
        }
    }

    private ItemStack icon(Item item) {
        return icons.computeIfAbsent(Loadout.idFromItem(item), key -> new ItemStack(item));
    }

    private String labelFor(String id) {
        Item item = Loadout.itemFromId(id);
        return item == null ? id : new ItemStack(item).getHoverName().getString();
    }

    private static int logicalFromMenu(int menuIndex) {
        if (menuIndex >= 5 && menuIndex <= 8) return 36 + (menuIndex - 5);
        if (menuIndex >= 9 && menuIndex <= 35) return menuIndex;
        if (menuIndex >= 36 && menuIndex <= 44) return menuIndex - 36;
        if (menuIndex == 45) return Loadout.OFFHAND;
        return -1;
    }

    private void status(String message) {
        statusMsg = message;
        statusTicks = 100;
    }
}
