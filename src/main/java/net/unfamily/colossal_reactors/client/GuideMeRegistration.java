package net.unfamily.colossal_reactors.client;

import guideme.Guide;
import guideme.GuideItemSettings;
import guideme.Guides;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.unfamily.colossal_reactors.ColossalReactors;

import java.util.concurrent.atomic.AtomicBoolean;

/** Registers the Colossal Reactors GuideME guide (client only). */
public final class GuideMeRegistration {
    private static final Identifier GUIDE_ID =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "guide");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private GuideMeRegistration() {}

    /**
     * Call from the mod constructor (see GuideME integration docs).
     * Must run before resource reload so pages are picked up on first load.
     */
    public static void register() {
        if (!ModList.get().isLoaded("guideme")) {
            return;
        }
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        try {
            Guide.builder(GUIDE_ID)
                    .itemSettings(GuideItemSettings.DEFAULT)
                    .build();
            ColossalReactors.LOGGER.info("GuideME guide registered ({})", GUIDE_ID);
        } catch (Exception e) {
            ColossalReactors.LOGGER.error("Failed to register GuideME guide", e);
            REGISTERED.set(false);
        }
    }

    public static ItemStack createGuideItemStack() {
        if (!ModList.get().isLoaded("guideme")) {
            return ItemStack.EMPTY;
        }
        return Guides.createGuideItem(GUIDE_ID);
    }
}
