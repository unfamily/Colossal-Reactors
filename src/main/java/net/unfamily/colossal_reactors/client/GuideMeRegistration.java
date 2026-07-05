package net.unfamily.colossal_reactors.client;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.unfamily.colossal_reactors.ColossalReactors;

/** Registers the Colossal Reactors GuideME guide (client only). */
public final class GuideMeRegistration {
    private static final String IMPL_CLASS =
            "net.unfamily.colossal_reactors.client.GuideMeRegistrationImpl";

    private GuideMeRegistration() {}

    /**
     * Call from the mod constructor (see GuideME integration docs).
     * Must run before resource reload so pages are picked up on first load.
     */
    public static void register() {
        if (FMLEnvironment.getDist() != Dist.CLIENT || !ModList.get().isLoaded("guideme")) {
            return;
        }
        try {
            Class.forName(IMPL_CLASS).getMethod("register").invoke(null);
        } catch (ReflectiveOperationException e) {
            ColossalReactors.LOGGER.error("Failed to register GuideME guide", e);
        }
    }

    public static ItemStack createGuideItemStack() {
        if (FMLEnvironment.getDist() != Dist.CLIENT || !ModList.get().isLoaded("guideme")) {
            return ItemStack.EMPTY;
        }
        try {
            return (ItemStack) Class.forName(IMPL_CLASS).getMethod("createGuideItemStack").invoke(null);
        } catch (ReflectiveOperationException e) {
            ColossalReactors.LOGGER.error("Failed to create GuideME guide item", e);
            return ItemStack.EMPTY;
        }
    }
}
