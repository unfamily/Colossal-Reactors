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
import net.unfamily.colossal_reactors.docs.ScriptsDocsGenerator;
import net.unfamily.colossal_reactors.fluid.FluidColorLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command: /colossal_reactors reload | dump
 */
@EventBusSubscriber(modid = ColossalReactors.MODID)
public class ColossalReactorsDebugCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("colossal_reactors")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(ColossalReactorsDebugCommand::executeReload))
                        .then(Commands.literal("dump")
                                .executes(ColossalReactorsDebugCommand::executeDump))
        );
    }

    private static int executeDump(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path reactorDir = Paths.get(basePath).resolve("reactor");
        try {
            ScriptsDocsGenerator.generateReadme(reactorDir);
            FuelLoader.dumpDefaultFile(reactorDir);
            CoolantLoader.dumpDefaultFile(reactorDir);
            HeatSinkLoader.dumpDefaultFile(reactorDir);
            FluidColorLoader.dumpDefaultFile(reactorDir);
            source.sendSuccess(() -> Component.literal("README.md and default_*.json written to reactor/: " + reactorDir.toAbsolutePath()), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Dump failed: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("Reloading reactor scripts (fuel + coolant + heat sink + fluid colors)..."), false);
        FuelLoader.scanConfigDirectory();
        CoolantLoader.scanConfigDirectory();
        HeatSinkLoader.scanConfigDirectory();
        FluidColorLoader.scanConfigDirectory();
        source.sendSuccess(() -> Component.literal("Reactor scripts reloaded."), false);
        return 1;
    }
}
