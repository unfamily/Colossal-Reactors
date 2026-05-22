package net.unfamily.colossal_reactors.fuel;

import org.jetbrains.annotations.Nullable;

/**
 * Parses {@link FuelDefinition#subType()} as {@code input-output}.
 */
public final class FuelSubType {

    private FuelSubType() {}

    public static FuelMedium parseInput(String subType) {
        String[] parts = split(subType);
        FuelMedium medium = FuelMedium.fromName(parts[0]);
        return medium != null ? medium : FuelMedium.ITEM;
    }

    public static FuelMedium parseOutput(String subType) {
        String[] parts = split(subType);
        FuelMedium medium = FuelMedium.fromName(parts[1]);
        return medium != null ? medium : FuelMedium.ITEM;
    }

    @Nullable
    public static String normalize(@Nullable String subType) {
        if (subType == null || subType.isBlank()) {
            return FuelDefinition.SUBTYPE_ITEM_ITEM;
        }
        String[] parts = split(subType);
        FuelMedium in = FuelMedium.fromName(parts[0]);
        FuelMedium out = FuelMedium.fromName(parts[1]);
        if (in == null || out == null) {
            return null;
        }
        return mediumName(in) + "-" + mediumName(out);
    }

    public static boolean inputsMatchSubType(String subType, java.util.List<String> inputs) {
        FuelMedium expected = parseInput(subType);
        for (String input : inputs) {
            if (input == null || input.isBlank()) {
                continue;
            }
            if (FuelMedium.fromSelector(input) != expected) {
                return false;
            }
        }
        return !inputs.isEmpty();
    }

    public static boolean outputMatchesSubType(String subType, @Nullable String output) {
        if (output == null || output.isBlank()) {
            return false;
        }
        return FuelMedium.fromSelector(output) == parseOutput(subType);
    }

    private static String[] split(String subType) {
        int dash = subType.indexOf('-');
        if (dash <= 0 || dash >= subType.length() - 1) {
            return new String[] { FuelMedium.NAME_ITEM, FuelMedium.NAME_ITEM };
        }
        return new String[] { subType.substring(0, dash), subType.substring(dash + 1) };
    }

    private static String mediumName(FuelMedium medium) {
        return switch (medium) {
            case ITEM -> FuelMedium.NAME_ITEM;
            case FLUID -> FuelMedium.NAME_FLUID;
            case CHEMICAL -> FuelMedium.NAME_CHEMICAL;
        };
    }
}
