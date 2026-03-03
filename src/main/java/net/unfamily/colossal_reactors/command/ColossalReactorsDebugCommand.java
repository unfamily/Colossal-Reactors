package net.unfamily.colossal_reactors.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Single command with two subcommands: /colossal_reactors reload | dump
 */
@EventBusSubscriber(modid = ColossalReactors.MODID)
public class ColossalReactorsDebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColossalReactorsDebugCommand.class);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("colossal_reactors")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(ColossalReactorsDebugCommand::executeReload))
                        .then(Commands.literal("dump")
                                .executes(ColossalReactorsDebugCommand::executeDumpDefault))
        );
    }

    private static int executeDumpDefault(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Dumping default reactor config files..."), false);
        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path reactorDir = Paths.get(basePath, "reactor");
        int ok = 0;
        try {
            FuelLoader.dumpDefaultFile(reactorDir);
            source.sendSuccess(() -> Component.literal("  Fuel default dumped"), false);
            ok++;
        } catch (Exception e) {
            LOGGER.error("Error dumping default fuel: {}", e.getMessage());
            source.sendFailure(Component.literal("  Fuel: " + e.getMessage()));
        }
        try {
            CoolantLoader.dumpDefaultFile(reactorDir);
            source.sendSuccess(() -> Component.literal("  Coolant default dumped"), false);
            ok++;
        } catch (Exception e) {
            LOGGER.error("Error dumping default coolant: {}", e.getMessage());
            source.sendFailure(Component.literal("  Coolant: " + e.getMessage()));
        }
        try {
            HeatSinkLoader.dumpDefaultFile(reactorDir);
            source.sendSuccess(() -> Component.literal("  Heat sink default dumped"), false);
            ok++;
        } catch (Exception e) {
            LOGGER.error("Error dumping default heat sink: {}", e.getMessage());
            source.sendFailure(Component.literal("  Heat sink: " + e.getMessage()));
        }
        source.sendSuccess(() -> Component.literal("Dump complete. Files in: " + reactorDir.toAbsolutePath()), false);
        return ok == 3 ? 1 : 0;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Reloading reactor scripts (fuel + coolant + heat sink)..."), false);
        FuelLoader.scanConfigDirectory();
        CoolantLoader.scanConfigDirectory();
        HeatSinkLoader.scanConfigDirectory();
        source.sendSuccess(() -> Component.literal("Reactor scripts reloaded."), false);
        return 1;
    }
}
