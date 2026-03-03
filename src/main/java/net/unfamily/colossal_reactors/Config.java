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
            .defineInRange("100_baseRfPerTick", 200.0, 0.0, 1000000.0);
    public static final ModConfigSpec.DoubleValue BASE_MB_PER_TICK = BUILDER
            .comment("Base fuel consumption in ingots per tick; converted to units using fuel definition unitsPerItem. Default: 0.03")
            .defineInRange("101_baseMbPerTick", 0.03, 0.0, 100.0);

    /** Max fuel units per rod (total capacity shared by all fuel types). Default: 10000. */
    public static final ModConfigSpec.IntValue ROD_MAX_FUEL_UNITS = BUILDER
            .comment("Max fuel units per reactor rod (shared capacity). Default: 10000")
            .defineInRange("107_rodMaxFuelUnits", 10000, 1, 10000000);

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
    /** Consumption scale: empirical base curve (this/sqrt(rodCount+1)); final consumption is divided by coolant mbMultiplier. Default 1.72. */
    public static final ModConfigSpec.DoubleValue CONSUMPTION_SCALE = BUILDER
            .comment("Consumption scale factor for empirical curve (fitted to experimental data). Final consumption also depends on coolant MB decrement %. Default: 1.72")
            .defineInRange("106b_consumptionScale", 1.72, 0.01, 10.0);
    /** Per horizontal neighbor (rod or shell): each rod's contribution is reduced by this factor. 0.25 = 4 neighbors -> 0 contribution. Default 0.25. */
    public static final ModConfigSpec.DoubleValue ROD_ADJACENCY_PENALTY = BUILDER
            .comment("Penalty per horizontal neighbor (another rod or border block). Each rod contributes max(0, 1 - penalty * adjacentCount). Default: 0.25")
            .defineInRange("106c_rodAdjacencyPenalty", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.IntValue REACTOR_VALIDATION_INTERVAL_TICKS = BUILDER
            .comment("Ticks between reactor structure re-validation when ON (e.g. 200 = 10s). Default: 200")
            .defineInRange("010_reactorValidationIntervalTicks", 200, 1, 1200);

    /** Steam produced per RF (e.g. 0.33 mB steam per 1 RF). Default: 0.33 */
    public static final ModConfigSpec.DoubleValue STEAM_PER_RF = BUILDER
            .comment("Steam (mB) produced per 1 RF. Default: 0.33")
            .defineInRange("108_steamPerRf", 0.33, 0.0, 10.0);

    static {
        BUILDER.comment("Port block settings").push("ports");
    }

    /** Resource Port: fluid tank capacity (mB). Default: 16000 */
    public static final ModConfigSpec.IntValue RESOURCE_PORT_TANK_CAPACITY_MB = BUILDER
            .comment("Resource Port: fluid tank capacity in mB. Default: 16000")
            .defineInRange("000", 16000, 1000, 1000000);

    /** Power Port: energy buffer capacity (FE). Default: 50M */
    public static final ModConfigSpec.IntValue POWER_PORT_CAPACITY = BUILDER
            .comment("Power Port: energy buffer capacity in FE. Default: 50000000")
            .defineInRange("010", 50000000, 1000, 1000000000);

    /** Power Port: max energy extract per tick (FE/t). Default: 1M */
    public static final ModConfigSpec.IntValue POWER_PORT_MAX_EXTRACT = BUILDER
            .comment("Power Port: max energy that can be extracted per tick (FE/t). Default: 1000000")
            .defineInRange("020", 1000000, 1, 100000000);

    static {
        BUILDER.pop();
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
