package net.unfamily.colossal_reactors;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Development and advanced configuration").push("dev");
    }

    public static final ModConfigSpec.ConfigValue<String> EXTERNAL_SCRIPTS_PATH = BUILDER
            .comment("Path to the external scripts directory for Colossal Reactors integration with KubeJS",
                    "Default: 'kubejs/external_scripts/colossal_reactors'",
                    "The system will look for scripts under this base path.")
            .define("000_external_scripts_path", "kubejs/external_scripts/colossal_reactors");

    /** When true, dumps full reactor validation steps to the log (info level). Default: false. */
    public static final ModConfigSpec.ConfigValue<Boolean> REACTOR_VALIDATION_DEBUG = BUILDER
            .comment("Dump full reactor validation to log (info level). Default: false")
            .define("001_reactor_validation_debug", false);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Reactor multiblock and simulation").push("reactor");
    }

    public static final ModConfigSpec.IntValue MAX_REACTOR_WIDTH = BUILDER
            .comment("Maximum reactor width (X or Z). Default: 64")
            .defineInRange("000_maxReactorWidth", 64, 1, 64);
    public static final ModConfigSpec.IntValue MAX_REACTOR_LENGTH = BUILDER
            .comment("Maximum reactor length (X or Z). Default: 64")
            .defineInRange("001_maxReactorLength", 64, 1, 64);
    public static final ModConfigSpec.IntValue MAX_REACTOR_HEIGHT = BUILDER
            .comment("Maximum reactor height (Y). Default: 32")
            .defineInRange("002_maxReactorHeight", 32, 1, 32);

    public static final ModConfigSpec.DoubleValue BASE_RF_PER_TICK = BUILDER
            .comment("Base RF/t for reactor formulas. Default: 200")
            .defineInRange("100_baseRfPerTick", 200.0, 0.0, 1_000_000.0);
    public static final ModConfigSpec.DoubleValue BASE_MB_PER_TICK = BUILDER
            .comment("Base MB/t for fuel consumption formulas. Default: 0.03")
            .defineInRange("101_baseMbPerTick", 0.03, 0.0, 100.0);

    public static final ModConfigSpec.IntValue URANIUM_INGOT_MB = BUILDER
            .comment("MB (fuel units) per one uranium ingot. Default: 1000")
            .defineInRange("102_uraniumIngotMb", 1000, 1, 1_000_000);

    public static final ModConfigSpec.DoubleValue RF_EFFICIENCY_LOSS = BUILDER
            .comment("RF efficiency loss factor (F3 in formulas). Default: 0.2")
            .defineInRange("103_rfEfficiencyLoss", 0.2, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue MB_EFFICIENCY_LOSS = BUILDER
            .comment("MB efficiency loss factor. Default: 1")
            .defineInRange("104_mbEfficiencyLoss", 1.0, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue PRODUCTION_MULTIPLIER = BUILDER
            .comment("Production multiplier (F5). Default: 1")
            .defineInRange("105_productionMultiplier", 1.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue CONSUMPTION_MULTIPLIER = BUILDER
            .comment("Consumption multiplier. Default: 1")
            .defineInRange("106_consumptionMultiplier", 1.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue REACTOR_VALIDATION_INTERVAL_TICKS = BUILDER
            .comment("Ticks between reactor structure re-validation (e.g. 100 = 5s). Default: 100")
            .defineInRange("010_reactorValidationIntervalTicks", 100, 1, 1200);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
