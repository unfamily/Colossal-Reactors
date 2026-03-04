package net.unfamily.colossal_reactors;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ========== dev ==========
    static {
        BUILDER.comment("Development and advanced configuration").push("dev");
    }

    public static final ModConfigSpec.ConfigValue<String> EXTERNAL_SCRIPTS_PATH = BUILDER
            .comment("Path to the external scripts directory for Colossal Reactors integration with KubeJS",
                    "Default: 'kubejs/external_scripts/colossal_reactors'")
            .define("000_external_scripts_path", "kubejs/external_scripts/colossal_reactors");

    public static final ModConfigSpec.ConfigValue<Boolean> REACTOR_VALIDATION_DEBUG = BUILDER
            .comment("Dump full reactor validation to log (info level). Default: false")
            .define("001_reactor_validation_debug", false);

    public static final ModConfigSpec.ConfigValue<Boolean> REACTOR_SIMULATION_DEBUG = BUILDER
            .comment("Log reactor simulation (heat sink rod/adj/nonAdj, weights, fuel/energy mult) each tick. Default: false")
            .define("002_reactor_simulation_debug", false);

    static {
        BUILDER.pop();
    }

    // ========== reactor ==========
    static {
        BUILDER.comment("Reactor multiblock and simulation").push("reactor");
    }

    // --- reactor.size ---
    static {
        BUILDER.comment("Maximum reactor dimensions").push("size");
    }

    public static final ModConfigSpec.IntValue MAX_REACTOR_WIDTH = BUILDER
            .comment("Maximum reactor width (X or Z). Default: 64")
            .defineInRange("000_maxReactorWidth", 65, 1, 1024);
    public static final ModConfigSpec.IntValue MAX_REACTOR_LENGTH = BUILDER
            .comment("Maximum reactor length (X or Z). Default: 64")
            .defineInRange("001_maxReactorLength", 65, 1, 1024);
    public static final ModConfigSpec.IntValue MAX_REACTOR_HEIGHT = BUILDER
            .comment("Maximum reactor height (Y). Default: 64")
            .defineInRange("002_maxReactorHeight", 65, 1, 1024);

    static {
        BUILDER.pop();
    }

    // --- reactor.base ---
    static {
        BUILDER.comment("Base values for RF production and fuel consumption").push("base");
    }

    public static final ModConfigSpec.DoubleValue BASE_RF_PER_TICK = BUILDER
            .comment("Base RF/t for reactor formulas. Default: 200")
            .defineInRange("100_baseRfPerTick", 200.0, 0.0, 1000000.0);
    public static final ModConfigSpec.DoubleValue BASE_FUEL_UNITS_PER_TICK = BUILDER
            .comment("Base fuel consumption (fuel units per tick scale). Default: 0.03")
            .defineInRange("101_baseFuelUnitsPerTick", 0.03, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue MIN_RF_PER_TICK = BUILDER
            .comment("Minimum RF/t produced by any running reactor. Default: 500")
            .defineInRange("101b_minRfPerTick", 500.0, 0.0, 1000000.0);
    public static final ModConfigSpec.DoubleValue MIN_FUEL_UNITS_PER_TICK = BUILDER
            .comment("Minimum fuel units consumed per tick. Default: 0.05")
            .defineInRange("101c_minFuelUnitsPerTick", 0.05, 0.0, 100.0);
    public static final ModConfigSpec.IntValue ROD_MAX_FUEL_UNITS = BUILDER
            .comment("Max fuel units per reactor rod (shared capacity). Default: 10000")
            .defineInRange("107_rodMaxFuelUnits", 10000, 1, 10000000);

    static {
        BUILDER.pop();
    }

    // --- reactor.efficiency ---
    static {
        BUILDER.comment("Efficiency and global multipliers").push("efficiency");
    }

    public static final ModConfigSpec.DoubleValue RF_EFFICIENCY_LOSS = BUILDER
            .comment("RF efficiency loss factor. Default: 0.2")
            .defineInRange("103_rfEfficiencyLoss", 0.2, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue FUEL_EFFICIENCY_LOSS = BUILDER
            .comment("Fuel efficiency factor. Default: 1")
            .defineInRange("104_fuelEfficiencyLoss", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue PRODUCTION_MULTIPLIER = BUILDER
            .comment("Production multiplier. Default: 1")
            .defineInRange("105_productionMultiplier", 1.0, 0.0, 100.0);
    public static final ModConfigSpec.DoubleValue CONSUMPTION_MULTIPLIER = BUILDER
            .comment("Consumption multiplier. Default: 1")
            .defineInRange("106_consumptionMultiplier", 1.0, 0.0, 100.0);

    static {
        BUILDER.pop();
    }

    // --- reactor.consumption ---
    static {
        BUILDER.comment("Fuel consumption curve (scale and size-based decay)").push("consumption");
    }

    public static final ModConfigSpec.DoubleValue CONSUMPTION_SCALE = BUILDER
            .comment("Consumption scale factor for empirical curve. Default: 1.72")
            .defineInRange("106b_consumptionScale", 1.72, 0.01, 10.0);
    public static final ModConfigSpec.DoubleValue CONSUMPTION_CURVE_DECAY_RODS = BUILDER
            .comment("As reactor grows, curve advantage fades. 0 = always sqrt; 400 = big reactors closer to linear. Default: 400")
            .defineInRange("106b2_consumptionCurveDecayRods", 400.0, 0.0, 10000.0);

    static {
        BUILDER.pop();
    }

    // --- reactor.rods ---
    static {
        BUILDER.comment("Rod adjacency (penalty for neighbors)").push("rods");
    }

    public static final ModConfigSpec.DoubleValue ROD_ADJACENCY_PENALTY = BUILDER
            .comment("Penalty per horizontal neighbor (rod or border). Default: 0.25")
            .defineInRange("106c_rodAdjacencyPenalty", 0.25, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    // --- reactor.heatSink ---
    static {
        BUILDER.comment("Heat sink (coolant block) weights and formula multipliers").push("heatSink");
    }

    public static final ModConfigSpec.DoubleValue HEAT_SINK_ADJACENT_WEIGHT = BUILDER
            .comment("Weight for coolant cells touching a rod (Refrigerante 0). Default: 1.0")
            .defineInRange("106d_heatSinkAdjacentWeight", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue HEAT_SINK_NON_ADJACENT_WEIGHT = BUILDER
            .comment("Weight for coolant cells not touching any rod (Refrigerante 1). Default: 0.5")
            .defineInRange("106e_heatSinkNonAdjacentWeight", 0.5, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue HEAT_SINK_CONSUMPTION_DIVISOR = BUILDER
            .comment("Extra divisor for fuel consumption with heat sinks (MB%%). Default: 1.35")
            .defineInRange("106f_heatSinkConsumptionDivisor", 1.35, 0.1, 10.0);
    public static final ModConfigSpec.DoubleValue HEAT_SINK_RF_MULTIPLIER = BUILDER
            .comment("Scale RF produced with heat sink formula. Default: 1.16")
            .defineInRange("106g_heatSinkRfMultiplier", 1.16, 0.1, 5.0);
    public static final ModConfigSpec.DoubleValue HEAT_SINK_FUEL_UNITS_MULTIPLIER = BUILDER
            .comment("Scale fuel consumption with heat sinks. Default: 1.21")
            .defineInRange("106h_heatSinkFuelUnitsMultiplier", 1.21, 0.1, 5.0);

    static {
        BUILDER.pop();
    }

    // --- reactor.validation ---
    static {
        BUILDER.comment("Structure validation").push("validation");
    }

    public static final ModConfigSpec.IntValue REACTOR_VALIDATION_INTERVAL_TICKS = BUILDER
            .comment("Ticks between reactor structure re-validation when ON (e.g. 200 = 10s). Default: 200")
            .defineInRange("010_reactorValidationIntervalTicks", 200, 1, 1200);
    public static final ModConfigSpec.BooleanValue ALLOW_MULTIPLE_REACTOR_CONTROLLERS = BUILDER
            .comment("If false, reactor is valid only with exactly one reactor controller on its outer faces. If true, multiple controllers allowed. Default: false")
            .define("011_allowMultipleReactorControllers", false);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.pop(); // reactor
    }

    // ========== ports ==========
    static {
        BUILDER.comment("Port block settings").push("ports");
    }

    // --- ports.resource ---
    static {
        BUILDER.comment("Resource Port").push("resource");
    }

    public static final ModConfigSpec.IntValue RESOURCE_PORT_TANK_CAPACITY_MB = BUILDER
            .comment("Fluid tank capacity in mB. Default: 16000")
            .defineInRange("000_resourcePortTankCapacityMb", 16000, 1000, 1000000);

    static {
        BUILDER.pop();
    }

    // --- ports.power ---
    static {
        BUILDER.comment("Power Port").push("power");
    }

    public static final ModConfigSpec.IntValue POWER_PORT_CAPACITY = BUILDER
            .comment("Energy buffer capacity in FE. Default: 50000000")
            .defineInRange("010_powerPortCapacity", 50000000, 1000, 1000000000);
    public static final ModConfigSpec.IntValue POWER_PORT_MAX_EXTRACT = BUILDER
            .comment("Max energy that can be extracted per tick (FE/t). Default: 1000000")
            .defineInRange("020_powerPortMaxExtract", 1000000, 1, 100000000);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.pop(); // ports
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
