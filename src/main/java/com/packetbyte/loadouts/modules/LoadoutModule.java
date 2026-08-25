package com.packetbyte.loadouts.modules;

import com.packetbyte.loadouts.Loadouts;
import com.packetbyte.loadouts.engine.ApplyEngine;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;

public class LoadoutModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> activeLoadout = sgGeneral.add(new StringSetting.Builder()
        .name("active-loadout")
        .description("The loadout used by the apply keybind.")
        .defaultValue("")
        .build()
    );

    private final Setting<Keybind> applyBind = sgGeneral.add(new KeybindSetting.Builder()
        .name("apply-bind")
        .description("Applies the active loadout when pressed.")
        .defaultValue(Keybind.none())
        .build()
    );

    private boolean bindWasPressed = false;

    public LoadoutModule() {
        super(Loadouts.CATEGORY, "loadouts", "Saves inventory layouts and sorts your items into place.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive()) return;

        boolean pressed = applyBind.get().isPressed();
        if (pressed && !bindWasPressed && !activeLoadout.get().isBlank()) {
            ApplyEngine.get().start(activeLoadout.get());
        }
        bindWasPressed = pressed;
    }

    public String getActiveName() {
        return activeLoadout.get();
    }

    public void setActiveName(String name) {
        activeLoadout.set(name);
        Systems.save();
    }
}
