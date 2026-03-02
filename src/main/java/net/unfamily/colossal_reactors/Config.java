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

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
