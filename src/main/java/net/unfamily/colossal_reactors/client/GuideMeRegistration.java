package net.unfamily.colossal_reactors.client;

import guideme.Guide;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers the GuideME guide on the client using a content folder that includes the optional
 * "Radiation Management & Instability" chapter only when relevant common-config toggles are on.
 */
public final class GuideMeRegistration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private GuideMeRegistration() {}

    /** Called once from client setup; safe if GuideME is absent at compile/runtime classpath handling. */
    public static void register() {
        if (!ModList.get().isLoaded("guideme")) {
            return;
        }
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        boolean optionalChapter = Boolean.TRUE.equals(Config.ENABLE_RADIATION_MANAGEMENT.get())
                || Boolean.TRUE.equals(Config.REACTOR_UNSTABILITY.get());
        String folder = optionalChapter
                ? "guides/colossal_reactors/guide"
                : "guides/colossal_reactors/guide_nominal";
        try {
            Guide.builder(ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "guide"))
                    .folder(folder)
                    .build();
            ColossalReactors.LOGGER.info("GuideME guide registered (folder={})", folder);
        } catch (Exception e) {
            ColossalReactors.LOGGER.warn("Failed to register GuideME guide: {}", e.getMessage());
            REGISTERED.set(false);
        }
    }
}
