package com.packetbyte.loadouts.gui;

import com.packetbyte.loadouts.data.ItemMatcher;
import com.packetbyte.loadouts.Loadouts;
import com.packetbyte.loadouts.data.Loadout;
import com.packetbyte.loadouts.data.LoadoutManager;
import com.packetbyte.loadouts.engine.ApplyEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class LoadoutScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final int PANEL_W = 120;

    private final Map<String, ItemStack> icons = new HashMap<>();

    private Loadout working;
    private String selectedName = "";

    private EditBox nameBox;
    private int panelX;

    public LoadoutScreen(String loadoutName) {
        super(player().inventoryMenu, player().getInventory(), Component.literal("Loadouts"));
        select(loadoutName);
    }

    private static Player player() {
        return Minecraft.getInstance().player;
    }

    @Override
    protected void init() {
        super.init();

        int colH = 16 + 4 + 18 + 4 + 18 + 4 + 18;
        panelX = leftPos + imageWidth + 6;
        int maxPanelX = this.width - PANEL_W - 4;
        if (panelX > maxPanelX) panelX = maxPanelX;

        int y = topPos + Math.max(4, (imageHeight - colH) / 2);

        nameBox = new EditBox(this.font, panelX, y, PANEL_W, 16, Component.literal("Loadout name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(selectedName);
        addRenderableWidget(nameBox);

        y += 20;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> save())
            .bounds(panelX, y, 58, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Capture"), b -> captureAll())
            .bounds(panelX + 62, y, 58, 18).build());

        y += 22;
        addRenderableWidget(Button.builder(Component.literal("New"), b -> newBlank())
            .bounds(panelX, y, 58, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteCurrent())
            .bounds(panelX + 62, y, 58, 18).build());

        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> apply())
            .bounds(panelX, y, PANEL_W, 18).build());

        Loadouts.LOG.info("LoadoutScreen layout: left={} top={} panelX={} w={} h={}",
            leftPos, topPos, panelX, this.width, this.height);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractBackground(g, mouseX, mouseY, delta);
        g.blit(RenderPipelines.GUI_TEXTURED, INVENTORY_LOCATION, leftPos, topPos, 0.0F, 0.0F,
            imageWidth, imageHeight, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        renderGhosts(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (ApplyEngine.get().isApplying() && hoveredSlot != null) return true;

        if (event.hasControlDown() && hoveredSlot != null
            && mc.player != null && mc.player.containerMenu == mc.player.inventoryMenu
            && !ApplyEngine.get().isApplying()) {

            int logical = logicalFromMenu(hoveredSlot.index);
            if (logical != -1) {
                ItemStack held = hoveredSlot.getItem();
                if (working.get(logical) != null) {
                    working.clear(logical);
                    hint("Removed from loadout.");
                } else if (!held.isEmpty()) {
                    working.set(logical, Loadout.idFromItem(held.getItem()));
                    hint("Added to loadout.");
                } else {
                    hint("That slot is empty.");
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    private void renderGhosts(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (mc.player == null) return;

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
    }

    private void save() {
        String name = currentName();
        if (name == null) {
            hint("Type a name first.");
            return;
        }

        working.name = name;
        LoadoutManager.save(working);
        selectedName = name;
        hint("Saved " + name + ".");
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
        hint("Copied your current inventory.");
    }

    private void newBlank() {
        working = new Loadout("");
        selectedName = "";
        nameBox.setValue("");
        hint("Blank loadout. Ctrl+click slots to mark them.");
    }

    private void deleteCurrent() {
        if (selectedName.isEmpty()) {
            hint("Nothing selected.");
            return;
        }

        if (LoadoutManager.delete(selectedName)) hint("Deleted " + selectedName + ".");
        else hint("Could not find " + selectedName + ".");

        selectedName = "";
        nameBox.setValue("");
        working = new Loadout("");
    }

    private void apply() {
        String name = currentName();
        if (name == null) {
            hint("Save it with a name before applying.");
            return;
        }

        working.name = name;
        LoadoutManager.save(working);
        selectedName = name;

        ApplyEngine.get().start(name);
    }

    private void hint(String message) {
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal(message));
    }
}
