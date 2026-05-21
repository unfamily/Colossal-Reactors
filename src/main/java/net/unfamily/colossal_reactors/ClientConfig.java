package net.unfamily.colossal_reactors;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only rendering settings (turbine rotor animation, reactor rod fill visuals).
 */
public final class ClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Reactor multiblock visuals (rod FILL blockstate updates on server in SP/integrated)")
                .push("reactor");
    }

    public static final ModConfigSpec.BooleanValue REACTOR_DISABLE_ROD_FILL_VISUAL_UPDATE = BUILDER
            .comment("When true, reactor rod FILL blockstates are not updated each tick (less lag, no fuel-level animation).",
                    "Read from client config in singleplayer/integrated; dedicated servers always update rod visuals. Default: false")
            .define("disable_rod_fill_visual_update", false);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Turbine rotor animation (client rendering only; does not affect RF/steam simulation)")
                .push("turbine");
    }

    public static final ModConfigSpec.BooleanValue TURBINE_ROTOR_ROTATION_ENABLED = BUILDER
            .comment("When false, turbine rods and blades use static block models only (no extra BER or model wrapping).",
                    "Useful on low-end PCs or very large turbines. Default: true")
            .define("enable_rotor_rotation", true);

    public static final ModConfigSpec.DoubleValue TURBINE_ROTOR_MAX_DEG_PER_TICK = BUILDER
            .comment("Maximum rotation speed in degrees per tick at full load (RF or steam vs estimated max). Default: 10")
            .defineInRange("rotor_max_deg_per_tick", 10.0, 0.1, 270.0);

    public static final ModConfigSpec.DoubleValue TURBINE_ROTOR_MIN_LOAD_TO_SPIN = BUILDER
            .comment("Minimum load factor (0-1) before the rotor starts spinning visually. Default: 0.01")
            .defineInRange("rotor_min_load_to_spin", 0.01, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Whether the server should tick reactor rod FILL blockstate updates. */
    public static boolean shouldUpdateReactorRodFillVisuals() {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            return true;
        }
        return !REACTOR_DISABLE_ROD_FILL_VISUAL_UPDATE.get();
    }

    private ClientConfig() {}
}
