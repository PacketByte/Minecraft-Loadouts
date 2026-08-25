package com.packetbyte.loadouts.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.packetbyte.loadouts.data.Loadout;
import com.packetbyte.loadouts.data.LoadoutManager;
import com.packetbyte.loadouts.gui.LoadoutEditorScreen;
import com.packetbyte.loadouts.modules.LoadoutModule;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.List;

public class LoadoutCommand extends Command {
    public LoadoutCommand() {
        super("loadout", "Manage inventory loadouts.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            info("Use (highlight).loadout <save|apply|edit|delete|list|set>(default). See the README for details.");
            return SINGLE_SUCCESS;
        });

        builder.then(literal("save").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            String name = StringArgumentType.getString(context, "name").trim();
            if (mc.player == null) {
                error("Join a world first.");
                return SINGLE_SUCCESS;
            }

            Loadout loadout = LoadoutManager.capture(mc.player, name);
            LoadoutManager.save(loadout);

            info("Saved loadout (highlight)%s(default) with %d item%s.", loadout.name, loadout.countFilled(), loadout.countFilled() == 1 ? "" : "s");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("apply").executes(context -> {
            module().requestApply(module().getActiveName());
            return SINGLE_SUCCESS;
        }).then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            module().requestApply(StringArgumentType.getString(context, "name").trim());
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("delete").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            String name = StringArgumentType.getString(context, "name").trim();
            if (LoadoutManager.delete(name)) info("Deleted loadout (highlight)%s(default).", name);
            else error("No loadout named (highlight)%s(default).", name);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(context -> {
            List<Loadout> all = LoadoutManager.all();
            if (all.isEmpty()) {
                info("No loadouts yet. Arrange your inventory, then use .loadout save <name>.");
                return SINGLE_SUCCESS;
            }

            String active = module().getActiveName();
            info("Loadouts (%d):", all.size());
            for (Loadout loadout : all) {
                String marker = loadout.name.equalsIgnoreCase(active) ? " (active)" : "";
                info("  (highlight)%s(default)%s - %d items", loadout.name, marker, loadout.countFilled());
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("set").then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            String name = StringArgumentType.getString(context, "name").trim();
            if (!LoadoutManager.exists(name)) {
                error("No loadout named (highlight)%s(default).", name);
                return SINGLE_SUCCESS;
            }

            module().setActiveName(LoadoutManager.get(name).name);
            info("Active loadout set to (highlight)%s(default).", LoadoutManager.get(name).name);
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("edit").executes(context -> {
            openEditor("");
            return SINGLE_SUCCESS;
        }).then(argument("name", StringArgumentType.greedyString()).executes(context -> {
            openEditor(StringArgumentType.getString(context, "name").trim());
            return SINGLE_SUCCESS;
        })));
    }

    private void openEditor(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            error("Join a world first.");
            return;
        }

        mc.setScreenAndShow(new LoadoutEditorScreen(name));
    }

    private LoadoutModule module() {
        return Modules.get().get(LoadoutModule.class);
    }
}
