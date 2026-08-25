package com.packetbyte.loadouts.gui;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screens.Screen;

public class LoadoutsTab extends Tab {
    public LoadoutsTab() {
        super("Loadouts");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new LoadoutsTabScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof LoadoutsTabScreen;
    }
}
