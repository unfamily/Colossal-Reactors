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
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;

/**
 * Command: /colossal_reactors reload
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
        );
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
