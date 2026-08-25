package com.packetbyte.loadouts.gui;

import com.packetbyte.loadouts.data.Loadout;
import com.packetbyte.loadouts.data.LoadoutManager;
import com.packetbyte.loadouts.modules.LoadoutModule;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadoutEditorScreen extends Screen {
    private static final int CELL = 20;
    private static final int HOTBAR_GAP = 4;

    private static final int COL_PANEL = 0xFF1A1C24;
    private static final int COL_CELL_EMPTY = 0xFF2A2D36;
    private static final int COL_CELL_FILLED = 0xFF343947;
    private static final int COL_CELL_BORDER = 0xFF14161C;
    private static final int COL_HOVER = 0x30FFFFFF;
    private static final int COL_SELECT = 0xFFFFD75A;
    private static final int COL_TEXT = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM = 0xFF9AA0AC;
    private static final int COL_CURRENT = 0xCC20232B;

    private final String[] editing = new String[Loadout.SIZE];

    private List<Loadout> saved = new ArrayList<>();
    private int savedIndex = -1;

    private EditBox nameBox;
    private EditBox searchBox;
    private String query = "";
    private List<Item> results = new ArrayList<>();
    private int resultPage;

    private int selectedCell = -1;
    private int dragOrigin = -1;
    private ItemStack dragging;

    private Map<String, ItemStack> previewStacks = new HashMap<>();

    private int contentX;
    private int contentY;
    private int contentW;

    private int gridW = 9 * CELL;
    private int gridH = 3 * CELL + HOTBAR_GAP + CELL;
    private int panelW;
    private int loadoutGridY;
    private int currentGridY;
    private int armorColX;
    private int resultsX;
    private int resultsW = 7 * CELL;
    private int resultsH;
    private int resultsCols = 6;

    public LoadoutEditorScreen(String name) {
        super(Component.literal("Loadouts"));

        refreshSaved();
        if (!name.isBlank()) {
            for (int i = 0; i < saved.size(); i++) {
                if (saved.get(i).name.equalsIgnoreCase(name)) {
                    savedIndex = i;
                    break;
                }
            }
        }

        if (savedIndex >= 0) {
            System.arraycopy(saved.get(savedIndex).items, 0, editing, 0, Loadout.SIZE);
        }
    }

    @Override
    protected void init() {
        panelW = gridW + 6 + CELL;
        contentW = panelW + 10 + resultsW;

        int rows = 4;
        int labelH = 11;
        int gapBetweenPanels = 14;
        int controlsH = 42;
        contentY = Math.max(6, (this.height - (rows * CELL + HOTBAR_GAP + 2 * labelH + gapBetweenPanels + controlsH)) / 2);
        contentX = Math.max(4, (this.width - contentW) / 2);

        loadoutGridY = contentY + labelH + 2;
        currentGridY = loadoutGridY + gridH + gapBetweenPanels + labelH + 2;
        armorColX = contentX + gridW + 6;

        resultsX = contentX + panelW + 10;
        int controlsY = currentGridY + gridH + 8;
        resultsH = controlsY - resultsY();

        nameBox = new EditBox(this.font, contentX + 18, controlsY, 130, 16, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setValue(savedIndex >= 0 ? saved.get(savedIndex).name : "");
        nameBox.setHint(Component.literal("loadout name"));
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleSaved(-1))
            .bounds(contentX, controlsY, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleSaved(1))
            .bounds(contentX + 150, controlsY, 16, 16).build());

        int bw = 54;
        int bgap = 4;
        int row2Y = controlsY + 20;
        String[] labels = {"New", "Save", "Delete", "Capture", "Clear", "Apply"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> onButton(idx))
                .bounds(contentX + i * (bw + bgap), row2Y, bw, 16).build());
        }

        int searchY = contentY + labelH + 2;
        searchBox = new EditBox(this.font, resultsX, searchY, resultsW, 16, Component.literal("Search"));
        searchBox.setMaxLength(48);
        searchBox.setHint(Component.literal("search items"));
        searchBox.setResponder(value -> {
            query = value.trim().toLowerCase();
            resultPage = 0;
            refreshResults();
        });
        addRenderableWidget(searchBox);

        refreshResults();
    }

    private int resultsY() {
        return contentY + 13;
    }

    private void refreshSaved() {
        saved = LoadoutManager.all();
        if (saved.isEmpty()) {
            savedIndex = -1;
            return;
        }
        if (savedIndex >= saved.size()) savedIndex = saved.size() - 1;
    }

    private void refreshResults() {
        results = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            String id = Loadout.idFromItem(item);
            if (id.equals("minecraft:air")) continue;
            if (query.isBlank() || id.contains(query)) results.add(item);
        }
        results.sort(Comparator.comparing(item -> Loadout.idFromItem(item)));
    }

    private void onButton(int which) {
        switch (which) {
            case 0 -> {
                for (int i = 0; i < Loadout.SIZE; i++) editing[i] = null;
                nameBox.setValue("");
                selectedCell = -1;
                savedIndex = -1;
            }
            case 1 -> saveCurrent();
            case 2 -> {
                String name = LoadoutManager.sanitizeName(nameBox.getValue());
                if (LoadoutManager.delete(name)) {
                    info("Deleted " + name);
                    refreshSaved();
                    if (!saved.isEmpty()) {
                        savedIndex = 0;
                        System.arraycopy(saved.get(0).items, 0, editing, 0, Loadout.SIZE);
                        nameBox.setValue(saved.get(0).name);
                    } else {
                        savedIndex = -1;
                    }
                }
            }
            case 3 -> captureInventory();
            case 4 -> {
                for (int i = 0; i < Loadout.SIZE; i++) editing[i] = null;
            }
            case 5 -> applyCurrent();
        }
    }

    private void saveCurrent() {
        String raw = nameBox.getValue();
        String name = LoadoutManager.sanitizeName(raw);

        Loadout loadout = new Loadout(name);
        System.arraycopy(editing, 0, loadout.items, 0, Loadout.SIZE);
        LoadoutManager.save(loadout);

        nameBox.setValue(name);
        refreshSaved();
        for (int i = 0; i < saved.size(); i++) {
            if (saved.get(i).name.equalsIgnoreCase(name)) {
                savedIndex = i;
                break;
            }
        }
    }

    private void captureInventory() {
        if (this.minecraft == null || this.minecraft.player == null) return;

        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            ItemStack stack = Loadout.stackAt(this.minecraft.player, slot);
            editing[slot] = stack.isEmpty() ? null : Loadout.idFromItem(stack.getItem());
        }
    }

    private void applyCurrent() {
        if (!(Modules.get().get(LoadoutModule.class)).isActive()) {
            Modules.get().get(LoadoutModule.class).toggle();
        }

        saveCurrent();
        LoadoutModule module = Modules.get().get(LoadoutModule.class);
        module.requestApply(LoadoutManager.sanitizeName(nameBox.getValue()));
        onClose();
    }

    private void cycleSaved(int dir) {
        refreshSaved();
        if (saved.isEmpty()) return;

        savedIndex = Math.floorMod(savedIndex + dir, saved.size());
        System.arraycopy(saved.get(savedIndex).items, 0, editing, 0, Loadout.SIZE);
        nameBox.setValue(saved.get(savedIndex).name);
        selectedCell = -1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0xA80D0F14);

        int panelH = currentGridY + gridH - contentY + 6;
        g.fill(contentX - 6, contentY - 4, contentX + contentW + 6, contentY + panelH, COL_PANEL);
        g.outline(contentX - 6, contentY - 4, contentX + contentW + 6, contentY + panelH, COL_CELL_BORDER);

        g.fill(resultsX - 4, resultsY(), resultsX + resultsW, resultsY() + resultsH, COL_PANEL);
        g.outline(resultsX - 4, resultsY(), resultsX + resultsW, resultsY() + resultsH, COL_CELL_BORDER);

        g.text(this.font, "Loadouts", contentX, contentY, COL_TEXT, true);
        g.text(this.font, "Search items", resultsX, contentY, COL_TEXT, true);

        g.text(this.font, "Loadout", contentX, loadoutGridY - 11, COL_TEXT_DIM, false);
        g.text(this.font, "Your inventory", contentX, currentGridY - 11, COL_TEXT_DIM, false);

        drawLoadoutGrid(g, mouseX, mouseY);
        drawCurrentGrid(g, mouseX, mouseY);
        drawResults(g, mouseX, mouseY);

        super.extractRenderState(g, mouseX, mouseY, delta);

        if (dragging != null) {
            g.item(dragging, mouseX - 8, mouseY - 8);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    }

    private void drawCell(GuiGraphicsExtractor g, int x, int y, String itemId, boolean dimmed,
                          boolean hovered, boolean selected) {
        int bg = itemId == null ? COL_CELL_EMPTY : COL_CELL_FILLED;
        g.fill(x, y, x + CELL, y + CELL, bg);
        g.outline(x, y, x + CELL, y + CELL, COL_CELL_BORDER);

        if (itemId != null) {
            ItemStack stack = preview(itemId);
            if (!stack.isEmpty()) {
                g.item(stack, x + 2, y + 2);
                if (dimmed) g.fill(x, y, x + CELL, y + CELL, COL_CURRENT);
            }
        }

        if (hovered) g.fill(x, y, x + CELL, y + CELL, COL_HOVER);
        if (selected) g.outline(x - 1, y - 1, x + CELL + 1, y + CELL + 1, COL_SELECT);
    }

    private void drawLoadoutGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            int[] pos = slotPos(slot);
            int x = contentX + pos[0];
            int y = pos[1];

            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            drawCell(g, x, y, editing[slot], false, hovered, slot == selectedCell);

            if (hovered) {
                if (editing[slot] != null) {
                    g.setTooltipForNextFrame(this.font, preview(editing[slot]), mouseX, mouseY);
                } else {
                    g.setTooltipForNextFrame(this.font, Component.literal("Pick an item on the right"), mouseX, mouseY);
                }
            }
        }
    }

    private void drawCurrentGrid(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            int[] pos = slotPos(slot);
            int x = contentX + pos[0];
            int y = pos[1];

            ItemStack stack = Loadout.stackAt(this.minecraft.player, slot);
            String itemId = stack.isEmpty() ? null : Loadout.idFromItem(stack.getItem());

            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            drawCell(g, x, y, itemId, true, hovered, false);

            if (hovered && !stack.isEmpty()) {
                g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }
    }

    private int[] slotPos(int slot) {
        int col = slot % 9;
        int rowBase = slot / 9;
        int row = rowBase <= 2 ? rowBase : 3;
        int extraGap = rowBase == 3 ? HOTBAR_GAP : 0;

        int x = col * CELL;
        int y = loadoutGridY + row * CELL + extraGap;
        if (slot >= Loadout.ARMOR_START && slot < Loadout.OFFHAND) {
            x = gridW + 6;
            y = loadoutGridY + (slot - Loadout.ARMOR_START) * CELL;
        } else if (slot == Loadout.OFFHAND) {
            x = gridW + 6;
            y = loadoutGridY + 4 * CELL + 4;
        }

        return new int[]{x, y};
    }

    private void drawResults(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int gridTop = resultsY() + 22;
        int usableH = resultsH - 30;
        int rowsShown = Math.max(1, usableH / CELL);

        int total = results.size();
        int perPage = rowsShown * resultsCols;
        int maxPage = Math.max(0, (total - 1) / Math.max(1, perPage));
        resultPage = Math.min(resultPage, maxPage);

        int start = resultPage * perPage;
        int end = Math.min(total, start + perPage);

        for (int i = start; i < end; i++) {
            int rel = i - start;
            int col = rel % resultsCols;
            int row = rel / resultsCols;
            int x = resultsX + 4 + col * CELL;
            int y = gridTop + row * CELL;

            Item item = results.get(i);
            ItemStack stack = preview(Loadout.idFromItem(item));

            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            g.fill(x, y, x + CELL, y + CELL, hovered ? 0xFF3E4453 : COL_CELL_EMPTY);
            g.item(stack, x + 2, y + 2);

            if (hovered) {
                g.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }

        String pageLabel = (resultPage + 1) + "/" + (maxPage + 1);
        g.text(this.font, pageLabel, resultsX + resultsW - 26, gridTop + rowsShown * CELL + 2, COL_TEXT_DIM, false);
        g.text(this.font, total + " items", resultsX + 4, gridTop + rowsShown * CELL + 2, COL_TEXT_DIM, false);
    }

    private int hitLoadoutCell(double mx, double my) {
        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            int[] pos = slotPos(slot);
            int x = contentX + pos[0];
            int y = pos[1];
            if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) return slot;
        }
        return -1;
    }

    private int hitResult(double mx, double my) {
        int gridTop = resultsY() + 22;
        if (mx < resultsX + 4 || mx >= resultsX + 4 + resultsCols * CELL || my < gridTop) return -1;

        int col = (int) ((mx - resultsX - 4) / CELL);
        int row = (int) ((my - gridTop) / CELL);

        int usableH = resultsH - 30;
        int rowsShown = Math.max(1, usableH / CELL);
        if (row < 0 || row >= rowsShown || col < 0 || col >= resultsCols) return -1;

        int index = resultPage * rowsShown * resultsCols + row * resultsCols + col;
        return index >= 0 && index < results.size() ? index : -1;
    }

    private int hitCurrentCell(double mx, double my) {
        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            int[] pos = slotPos(slot);
            int x = contentX + pos[0];
            int y = pos[1] - loadoutGridY + currentGridY;
            if (mx >= x && mx < x + CELL && my >= y && my < y + CELL) return slot;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();

        int cell = hitLoadoutCell(mx, my);
        if (cell != -1) {
            if (button == 1) {
                editing[cell] = null;
                return true;
            }
            if (button == 0 && editing[cell] != null) {
                dragOrigin = cell;
                dragging = preview(editing[cell]);
                return true;
            }
            selectedCell = cell;
            return true;
        }

        int result = hitResult(mx, my);
        if (result != -1 && button == 0) {
            int target = selectedCell != -1 ? selectedCell : nextEmptyCell();
            if (target != -1) {
                editing[target] = Loadout.idFromItem(results.get(result));
                selectedCell = target;
            }
            return true;
        }

        int current = hitCurrentCell(mx, my);
        if (current != -1 && button == 0 && this.minecraft != null && this.minecraft.player != null) {
            ItemStack stack = Loadout.stackAt(this.minecraft.player, current);
            if (!stack.isEmpty()) {
                dragOrigin = -1;
                dragging = stack.copy();
                return true;
            }
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            int cell = hitLoadoutCell(event.x(), event.y());
            if (cell != -1) {
                editing[cell] = Loadout.idFromItem(dragging.getItem());
                if (dragOrigin != -1 && dragOrigin != cell) editing[dragOrigin] = null;
                selectedCell = cell;
            }
            dragging = null;
            dragOrigin = -1;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= resultsX - 4 && mouseX <= resultsX + resultsW) {
            if (scrollY > 0) resultPage--;
            else resultPage++;
            if (resultPage < 0) resultPage = 0;
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int nextEmptyCell() {
        for (int slot = 0; slot < Loadout.SIZE; slot++) {
            if (editing[slot] == null) return slot;
        }
        return -1;
    }

    private ItemStack preview(String itemId) {
        return previewStacks.computeIfAbsent(itemId, id -> {
            Item item = Loadout.itemFromId(id);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        });
    }

    private void info(String message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.sendSystemMessage(Component.literal(message));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
