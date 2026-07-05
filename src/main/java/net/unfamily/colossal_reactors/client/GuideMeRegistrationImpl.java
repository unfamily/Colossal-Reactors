package net.unfamily.colossal_reactors.client;

import guideme.Guide;
import guideme.GuideItemSettings;
import guideme.Guides;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;

import java.util.concurrent.atomic.AtomicBoolean;

/** GuideME integration loaded only when the guideme mod is present. */
public final class GuideMeRegistrationImpl {
    private static final ResourceLocation GUIDE_ID =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "guide");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private GuideMeRegistrationImpl() {}

    public static void register() {
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
            throw e;
        }
    }

    public static ItemStack createGuideItemStack() {
        return Guides.createGuideItem(GUIDE_ID);
    }
}
