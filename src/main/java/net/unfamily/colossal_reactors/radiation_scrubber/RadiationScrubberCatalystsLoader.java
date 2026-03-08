package net.unfamily.colossal_reactors.radiation_scrubber;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads Radiation Scrubber catalyst definitions from datapack (type colossal_reactors:radiation_scrubber_catalysts).
 * Same format as fuel/coolant/heat_sinks: "entries" array. Each entry has "catalysts" (item id or #tag), optional "mult" (removal = base * mult).
 * Radius is config-only (blocks). Multiple files/entries merged: catalysts concatenated, last mult wins.
 */
public final class RadiationScrubberCatalystsLoader {

    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_CATALYSTS = "catalysts";
    private static final String KEY_MULT = "mult";

    private static List<String> catalysts = new ArrayList<>();
    private static int effectiveness = 10;

    /** Applied by ReactorDataReloadListener after merging all datapack files. */
    public static void applyLoaded(List<String> catalystsList, int multValue) {
        catalysts = catalystsList != null ? new ArrayList<>(catalystsList) : new ArrayList<>();
        effectiveness = Math.max(1, multValue);
    }

    public static List<String> getCatalysts() {
        return Collections.unmodifiableList(catalysts);
    }

    public static int getEffectiveness() {
        return effectiveness;
    }

    /**
     * Parses one JSON root. Expects "type": "colossal_reactors:radiation_scrubber_catalysts",
     * "entries": [ { "catalysts": ["#c:dusts/boron"], "mult": 10 } ]. Removal formula: base_radiation_removal * mult.
     */
    @Nullable
    public static ParsedCatalysts parseFromRoot(JsonObject root, String source) {
        if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) return null;
        List<String> list = new ArrayList<>();
        int mult = -1;
        for (JsonElement entryEl : root.getAsJsonArray(KEY_ENTRIES)) {
            if (!entryEl.isJsonObject()) continue;
            JsonObject entry = entryEl.getAsJsonObject();
            if (entry.has(KEY_CATALYSTS) && entry.get(KEY_CATALYSTS).isJsonArray()) {
                for (JsonElement e : entry.getAsJsonArray(KEY_CATALYSTS)) {
                    String s = e.getAsString();
                    if (s != null && !s.isBlank()) list.add(s.trim());
                }
            }
            if (entry.has(KEY_MULT)) mult = entry.get(KEY_MULT).getAsInt();
        }
        return new ParsedCatalysts(list, mult);
    }

    public record ParsedCatalysts(List<String> catalysts, int mult) {}
}
