package com.packetbyte.loadouts.gui;

import com.packetbyte.loadouts.data.Loadout;
import com.packetbyte.loadouts.data.LoadoutManager;
import com.packetbyte.loadouts.engine.ApplyEngine;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class LoadoutsTabScreen extends WindowTabScreen {
    private final Minecraft mc = Minecraft.getInstance();

    private WTable table;
    private WTextBox nameBox;

    public LoadoutsTabScreen(GuiTheme theme, Tab tab) {
        super(theme, tab);
    }

    @Override
    public void initWidgets() {
        table = add(theme.table()).expandX().widget();
        fillTable();

        add(theme.horizontalSeparator()).expandX();

        WTable bottom = add(theme.table()).expandX().widget();
        nameBox = bottom.add(theme.textBox("")).expandCellX().widget();
        WButton capture = bottom.add(theme.button("Capture current")).widget();
        capture.action = () -> {
            String name = LoadoutManager.sanitizeName(nameBox.get().trim());
            if (name.isEmpty()) return;

            if (LoadoutManager.exists(name)) {
                info("A loadout called " + name + " already exists.");
                return;
            }

            if (mc.player == null) {
                info("Join a world first.");
                return;
            }

            LoadoutManager.capture(mc.player, name);
            reload();
        };

        WButton openEditor = bottom.add(theme.button("New blank")).widget();
        openEditor.action = () -> mc.setScreenAndShow(new LoadoutScreen(""));
    }

    private void fillTable() {
        List<Loadout> loadouts = LoadoutManager.all();
        if (loadouts.isEmpty()) {
            table.add(theme.label("No loadouts yet"));
            table.row();
            return;
        }

        for (Loadout loadout : loadouts) {
            String name = loadout.name;

            table.add(theme.label(name + " (" + loadout.countFilled() + ")")).expandCellX();

            WButton apply = table.add(theme.button("Apply")).widget();
            apply.action = () -> applyLoadout(name);

            WButton edit = table.add(theme.button("Edit")).widget();
            edit.action = () -> mc.setScreenAndShow(new LoadoutScreen(name));

            WConfirmedMinus delete = table.add(theme.confirmedMinus()).widget();
            delete.action = () -> {
                LoadoutManager.delete(name);
                reload();
            };

            table.row();
        }
    }

    private void applyLoadout(String name) {
        if (mc.player == null) {
            info("Join a world first.");
            return;
        }

        ApplyEngine.get().start(name);
        mc.setScreenAndShow(null);
    }

    private void info(String message) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(message));
    }
}
