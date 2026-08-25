package com.packetbyte.loadouts;

import com.packetbyte.loadouts.commands.LoadoutCommand;
import com.packetbyte.loadouts.data.LoadoutManager;
import com.packetbyte.loadouts.gui.LoadoutsTab;
import com.packetbyte.loadouts.modules.LoadoutModule;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Loadouts extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Loadouts");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Loadouts");

        LoadoutManager.init();

        Modules.get().add(new LoadoutModule());
        Commands.add(new LoadoutCommand());
        Tabs.add(new LoadoutsTab());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.packetbyte.loadouts";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("PacketByte", "Minecraft-Loadouts");
    }
}
