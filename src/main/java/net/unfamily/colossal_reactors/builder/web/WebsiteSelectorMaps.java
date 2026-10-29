package net.unfamily.colossal_reactors.builder.web;

import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Parses config CSV rows {@code datapack_selector;website_id} for ModDecoded website material maps.
 * <p>
 * Seed tokens use {@code ~} instead of {@code #} for tags (URL hash-safe), matching ModDecoded
 * {@code coilSel}/{@code sinkSel}. CSV maps are optional aliases for short mdct1/mdcr1 ids.
 */
public final class WebsiteSelectorMaps {

    public record Entry(String selector, String websiteId) {}

    private WebsiteSelectorMaps() {}

    public static List<Entry> coilEntries() {
        return parse(Config.WEBSITE_COIL_SELECTOR_MAP.get());
    }

    public static List<Entry> heatsinkEntries() {
        return parse(Config.WEBSITE_HEATSINK_SELECTOR_MAP.get());
    }

    public static List<Entry> parse(@Nullable List<? extends String> lines) {
        List<Entry> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (Object raw : lines) {
            if (!(raw instanceof String line) || line.isBlank()) {
                continue;
            }
            Entry e = parseLine(line.trim());
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    @Nullable
    public static Entry parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        int sep = line.indexOf(';');
        if (sep <= 0 || sep >= line.length() - 1) {
            return null;
        }
        String selector = line.substring(0, sep).trim();
        String websiteId = line.substring(sep + 1).trim();
        if (selector.isEmpty() || websiteId.isEmpty()) {
            return null;
        }
        return new Entry(selector, websiteId);
    }

    /**
     * Seed token → datapack selector: {@code ~c:…} → {@code #c:…}; block/fluid ids and short ids unchanged.
     */
    public static String toDatapackSelector(@Nullable String token) {
        if (token == null || token.isEmpty()) {
            return "";
        }
        if (token.startsWith("~")) {
            return "#" + token.substring(1);
        }
        return token;
    }

    /**
     * Datapack selector → seed token: {@code #c:…} → {@code ~c:…}; block/fluid ids unchanged.
     */
    public static String toSeedSelector(@Nullable String datapackSel) {
        if (datapackSel == null || datapackSel.isEmpty()) {
            return "";
        }
        if (datapackSel.startsWith("#")) {
            return "~" + datapackSel.substring(1);
        }
        return datapackSel;
    }

    /**
     * True for tokens allowed in mdct2/mdcr2 material fields: {@code air}, short id,
     * {@code namespace:path}, or {@code ~namespace:…}. Must not contain {@code .} or {@code ,}.
     */
    public static boolean isSeedMaterialToken(@Nullable String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        if (s.indexOf('.') >= 0 || s.indexOf(',') >= 0) {
            return false;
        }
        if ("air".equals(s) || "rod".equals(s)) {
            return true;
        }
        if (s.startsWith("~")) {
            String rest = s.substring(1);
            return !rest.isEmpty() && rest.indexOf(':') > 0
                    && rest.matches("[a-z0-9_.:/-]+");
        }
        if (s.indexOf(':') > 0) {
            return s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
        }
        return s.matches("[a-z][a-z0-9_]*");
    }

    /** Short coil ids from {@code website.000_coil_selector_map} (mdct1 table); empty if CSV unused. */
    public static Set<String> shortCoilIds() {
        Set<String> out = new LinkedHashSet<>();
        out.add("air");
        for (Entry e : coilEntries()) {
            if (e.websiteId() != null && e.websiteId().matches("[a-z][a-z0-9_]*")) {
                out.add(e.websiteId().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    /** Short heat-sink ids from {@code website.001_heatsink_selector_map} (mdcr1 table). */
    public static Set<String> shortHeatSinkIds() {
        Set<String> out = new LinkedHashSet<>();
        out.add("air");
        for (Entry e : heatsinkEntries()) {
            if (e.websiteId() != null && e.websiteId().matches("[a-z][a-z0-9_]*")) {
                out.add(e.websiteId().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    public static boolean isShortCoilId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return shortCoilIds().contains(id.toLowerCase(Locale.ROOT));
    }

    public static boolean isShortHeatSinkId(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return shortHeatSinkIds().contains(id.toLowerCase(Locale.ROOT));
    }

    /** Website coil id → datapack selector from config, if listed. */
    public static Optional<String> coilSelectorForWebsiteId(String websiteId) {
        return selectorForWebsiteId(coilEntries(), websiteId);
    }

    /** Website heatsink id → datapack selector/hint from config, if listed. */
    public static Optional<String> heatsinkSelectorForWebsiteId(String websiteId) {
        return selectorForWebsiteId(heatsinkEntries(), websiteId);
    }

    private static Optional<String> selectorForWebsiteId(List<Entry> entries, String websiteId) {
        if (websiteId == null || websiteId.isEmpty()) {
            return Optional.empty();
        }
        String want = websiteId.toLowerCase(Locale.ROOT);
        for (Entry e : entries) {
            if (e.websiteId().equalsIgnoreCase(want)) {
                return Optional.of(e.selector());
            }
        }
        return Optional.empty();
    }

    /**
     * Website id for a datapack selector (exact match preferred, then path-tail / contains).
     */
    public static Optional<String> websiteIdForCoilSelector(String selector) {
        return websiteIdForSelector(coilEntries(), selector);
    }

    public static Optional<String> websiteIdForHeatsinkSelector(String selector) {
        return websiteIdForSelector(heatsinkEntries(), selector);
    }

    private static Optional<String> websiteIdForSelector(List<Entry> entries, String selector) {
        if (selector == null || selector.isEmpty()) {
            return Optional.empty();
        }
        String s = selector.toLowerCase(Locale.ROOT);
        // Also try seed-form (~) as if it were a datapack tag.
        String datapack = toDatapackSelector(s).toLowerCase(Locale.ROOT);
        for (Entry e : entries) {
            if (e.selector().equalsIgnoreCase(s) || e.selector().equalsIgnoreCase(datapack)) {
                return Optional.of(e.websiteId());
            }
        }
        String path = pathTail(datapack);
        for (Entry e : entries) {
            String ep = pathTail(e.selector().toLowerCase(Locale.ROOT));
            if (ep.equals(path) || datapack.contains(ep) || e.selector().toLowerCase(Locale.ROOT).contains(path)) {
                return Optional.of(e.websiteId());
            }
        }
        return Optional.empty();
    }

    /** First non-air elec-coil builder option index (0 if none). */
    public static int firstNonAirCoilOptionIndex() {
        List<ElecCoilDefinition> defs = ElecCoilLoader.getAllDefinitions();
        for (int i = 0; i < defs.size(); i++) {
            if (!ElecCoilLoader.isAirOnlyDefinition(defs.get(i))) {
                return i;
            }
        }
        return 0;
    }

    /** First non-air heat-sink builder option index (0 = air). */
    public static int firstNonAirHeatSinkOptionIndex() {
        int count = HeatSinkLoader.getHeatSinkOptionCount();
        for (int opt = 1; opt < count; opt++) {
            HeatSinkDefinition def = HeatSinkLoader.definitionForOptionIndex(opt);
            if (def != null) {
                return opt;
            }
        }
        return 0;
    }

    /** Website token for the first non-air coil mapping, or {@code air}. */
    public static String firstNonAirCoilWebsiteId() {
        for (Entry e : coilEntries()) {
            if (!"air".equalsIgnoreCase(e.websiteId()) && !"minecraft:air".equalsIgnoreCase(e.selector())) {
                return e.websiteId();
            }
        }
        int idx = firstNonAirCoilOptionIndex();
        List<ElecCoilDefinition> defs = ElecCoilLoader.getAllDefinitions();
        if (idx >= 0 && idx < defs.size() && defs.get(idx) != null && !defs.get(idx).validBlocks().isEmpty()) {
            String sel = defs.get(idx).validBlocks().get(0);
            return websiteIdForCoilSelector(sel).orElseGet(() -> toSeedSelector(sel));
        }
        return "air";
    }

    /**
     * Stable short token derived from a datapack selector when no config map entry exists.
     * Prefer {@link #toSeedSelector(String)} for mdct2/mdcr2 fields.
     */
    @Nullable
    public static String websiteTokenFromSelector(String selector) {
        if (selector == null || selector.isEmpty()) {
            return null;
        }
        String token = pathTail(selector).toLowerCase(Locale.ROOT).replace('-', '_');
        if ("air".equals(token)) {
            return "air";
        }
        if (token.isEmpty() || !token.matches("[a-z][a-z0-9_]*")) {
            return null;
        }
        return token;
    }

    private static String pathTail(String selector) {
        String s = selector;
        if (s.startsWith("#") || s.startsWith("~")) {
            s = s.substring(1);
        }
        if (s.contains(":")) {
            s = s.substring(s.indexOf(':') + 1);
        }
        if (s.contains("/")) {
            s = s.substring(s.lastIndexOf('/') + 1);
        }
        return s;
    }
}
