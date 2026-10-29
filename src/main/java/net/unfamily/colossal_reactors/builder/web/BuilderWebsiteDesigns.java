package net.unfamily.colossal_reactors.builder.web;

import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.reactor.RodPatternLogic;
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.turbine.TurbineRodPatternLogic;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parse helpers and apply paths for ModDecoded website design seeds on builders.
 * Material maps live in {@link net.unfamily.colossal_reactors.Config} (ACM CSV tables).
 */
public final class BuilderWebsiteDesigns {

    private BuilderWebsiteDesigns() {}

    public static Optional<ModDecodedReactorDesign> parseReactor(String raw) {
        return ModDecodedReactorDesign.decode(raw);
    }

    public static Optional<ModDecodedTurbineDesign> parseTurbine(String raw) {
        return ModDecodedTurbineDesign.decode(raw);
    }

    /**
     * Applies a decoded reactor design: centered exterior size, WEBSITE pattern, PRODUCTION mode,
     * coolant/fuel simulation indices, preferred heat-sink option, and stored seed on the BE.
     */
    public static boolean applyToReactorBuilder(ReactorBuilderBlockEntity builder, String raw) {
        Optional<ModDecodedReactorDesign> parsed = parseReactor(raw);
        if (parsed.isEmpty()) {
            if (builder != null) {
                builder.setWebsiteDesign(raw == null ? "" : raw.trim());
            }
            return false;
        }
        return applyToReactorBuilder(builder, parsed.get());
    }

    public static boolean applyToReactorBuilder(ReactorBuilderBlockEntity builder, ModDecodedReactorDesign design) {
        if (builder == null || design == null) {
            return false;
        }
        builder.setExteriorSizeCentered(design.exteriorX(), design.exteriorZ(), design.exteriorY());
        builder.setRodPattern(RodPatternLogic.PATTERN_WEBSITE);
        builder.setPatternMode(RodPatternLogic.MODE_PRODUCTION);
        applyReactorFuel(builder, design.fuel());
        applyReactorCoolant(builder, design.coolant());
        int sinkIdx = preferredHeatSinkOption(design);
        if (sinkIdx >= 0) {
            builder.setSelectedHeatSinkIndex(sinkIdx);
        }
        builder.setWebsiteDesign(design.code());
        return builder.hasValidWebsiteDesign();
    }

    /**
     * Applies a decoded turbine design: centered size, WEBSITE pattern, coil layers/type, and rod mask.
     * Coil fill {@code o} ({@link ModDecodedTurbineDesign#coilFillOne()}) is ignored on purpose —
     * build and Website sim always fill the entire coil zone (ModDecoded builder contract).
     */
    public static boolean applyToTurbineBuilder(TurbineBuilderBlockEntity builder, String raw) {
        Optional<ModDecodedTurbineDesign> parsed = parseTurbine(raw);
        if (parsed.isEmpty()) {
            if (builder != null) {
                builder.setWebsiteDesign(raw == null ? "" : raw.trim());
            }
            return false;
        }
        return applyToTurbineBuilder(builder, parsed.get());
    }

    /** @see #applyToTurbineBuilder(TurbineBuilderBlockEntity, String) for coil-fill {@code o} policy */
    public static boolean applyToTurbineBuilder(TurbineBuilderBlockEntity builder, ModDecodedTurbineDesign design) {
        if (builder == null || design == null) {
            return false;
        }
        builder.setExteriorSizeCentered(design.exteriorX(), design.exteriorZ(), design.exteriorY());
        builder.setRodPattern(TurbineRodPatternLogic.PATTERN_WEBSITE);
        builder.setCoilLayerCount(design.coilLayers());
        int coilIdx = findCoilOptionIndex(design.coilId());
        if (coilIdx >= 0) {
            builder.setSelectedCoilIndex(coilIdx);
        }
        int rodCoilIdx = findCoilOptionIndex(design.effectiveRodCoilId());
        if (rodCoilIdx >= 0) {
            builder.setSelectedRodCoilIndex(rodCoilIdx);
        } else if (coilIdx >= 0) {
            builder.setSelectedRodCoilIndex(coilIdx);
        }
        builder.setWebsiteDesign(design.code());
        return builder.hasValidWebsiteDesign();
    }

    /**
     * Heat-sink builder option for a ModDecoded sink id or seed selector ({@code ~c:…} / {@code ns:path}).
     * CSV short ids are optional aliases. Unknown → first non-air heatsink.
     */
    public static int findHeatSinkOptionIndex(String websiteSinkId) {
        if (websiteSinkId == null || websiteSinkId.isEmpty() || "rod".equals(websiteSinkId)) {
            return WebsiteSelectorMaps.firstNonAirHeatSinkOptionIndex();
        }
        if ("air".equals(websiteSinkId) || "minecraft:air".equals(websiteSinkId)) {
            return 0;
        }
        String datapack = WebsiteSelectorMaps.toDatapackSelector(websiteSinkId);
        int optionCount = HeatSinkLoader.getHeatSinkOptionCount();
        for (int opt = 1; opt < optionCount; opt++) {
            HeatSinkDefinition def = HeatSinkLoader.definitionForOptionIndex(opt);
            if (def == null) {
                continue;
            }
            if (selectorsMatchExact(def.validBlocks(), datapack) || selectorsMatchExact(def.validLiquids(), datapack)) {
                return opt;
            }
        }
        String hint = WebsiteSelectorMaps.heatsinkSelectorForWebsiteId(websiteSinkId).orElse(datapack);
        for (int opt = 1; opt < optionCount; opt++) {
            HeatSinkDefinition def = HeatSinkLoader.definitionForOptionIndex(opt);
            if (def == null) {
                continue;
            }
            if (selectorsMatchExact(def.validBlocks(), hint) || selectorsMatchExact(def.validLiquids(), hint)
                    || selectorsContainHint(def.validBlocks(), hint) || selectorsContainHint(def.validLiquids(), hint)) {
                return opt;
            }
        }
        return WebsiteSelectorMaps.firstNonAirHeatSinkOptionIndex();
    }

    /**
     * Coil builder option for a ModDecoded coil id or seed selector ({@code air} → 0).
     * Exact datapack match first; CSV short-id alias optional. Unknown → first non-air coil.
     */
    public static int findCoilOptionIndex(String websiteCoilId) {
        if (websiteCoilId == null || websiteCoilId.isEmpty()) {
            return WebsiteSelectorMaps.firstNonAirCoilOptionIndex();
        }
        if ("air".equals(websiteCoilId) || "minecraft:air".equals(websiteCoilId)) {
            return 0;
        }
        String datapack = WebsiteSelectorMaps.toDatapackSelector(websiteCoilId);
        List<ElecCoilDefinition> defs = ElecCoilLoader.getAllDefinitions();
        for (int i = 0; i < defs.size(); i++) {
            ElecCoilDefinition def = defs.get(i);
            if (ElecCoilLoader.isAirOnlyDefinition(def)) {
                continue;
            }
            if (selectorsMatchExact(def.validBlocks(), datapack)) {
                return i;
            }
        }
        String hint = WebsiteSelectorMaps.coilSelectorForWebsiteId(websiteCoilId).orElse(datapack);
        for (int i = 0; i < defs.size(); i++) {
            ElecCoilDefinition def = defs.get(i);
            if (ElecCoilLoader.isAirOnlyDefinition(def)) {
                continue;
            }
            if (selectorsMatchExact(def.validBlocks(), hint) || selectorsContainHint(def.validBlocks(), hint)) {
                return i;
            }
        }
        return WebsiteSelectorMaps.firstNonAirCoilOptionIndex();
    }

    /**
     * Seed coil token for a builder option: short CSV id when mapped (mdct1-friendly),
     * otherwise {@link WebsiteSelectorMaps#toSeedSelector(String)} of the first {@code valid_blocks} entry.
     */
    public static String websiteCoilIdForOptionIndex(int optionIndex) {
        if (optionIndex <= 0) {
            return "air";
        }
        List<ElecCoilDefinition> defs = ElecCoilLoader.getAllDefinitions();
        if (optionIndex < 0 || optionIndex >= defs.size()) {
            return WebsiteSelectorMaps.firstNonAirCoilWebsiteId();
        }
        ElecCoilDefinition def = defs.get(optionIndex);
        if (def == null || ElecCoilLoader.isAirOnlyDefinition(def) || def.validBlocks().isEmpty()) {
            return "air";
        }
        for (String sel : def.validBlocks()) {
            Optional<String> id = WebsiteSelectorMaps.websiteIdForCoilSelector(sel);
            if (id.isPresent()) {
                return id.get();
            }
        }
        return WebsiteSelectorMaps.toSeedSelector(def.validBlocks().get(0));
    }

    /**
     * Seed heat-sink token for a builder option: short CSV id when mapped, else seed selector.
     */
    public static String websiteHeatSinkIdForOptionIndex(int optionIndex) {
        if (optionIndex <= 0) {
            return "air";
        }
        HeatSinkDefinition def = HeatSinkLoader.definitionForOptionIndex(optionIndex);
        if (def == null) {
            return "air";
        }
        List<String> blocks = def.validBlocks();
        List<String> liquids = def.validLiquids();
        if (blocks != null) {
            for (String sel : blocks) {
                Optional<String> id = WebsiteSelectorMaps.websiteIdForHeatsinkSelector(sel);
                if (id.isPresent()) {
                    return id.get();
                }
            }
        }
        if (liquids != null) {
            for (String sel : liquids) {
                Optional<String> id = WebsiteSelectorMaps.websiteIdForHeatsinkSelector(sel);
                if (id.isPresent()) {
                    return id.get();
                }
            }
        }
        if (blocks != null && !blocks.isEmpty()) {
            return WebsiteSelectorMaps.toSeedSelector(blocks.get(0));
        }
        if (liquids != null && !liquids.isEmpty()) {
            return WebsiteSelectorMaps.toSeedSelector(liquids.get(0));
        }
        return "air";
    }

    private static boolean selectorsMatchExact(List<String> selectors, String hint) {
        if (selectors == null || hint == null || hint.isEmpty()) {
            return false;
        }
        String h = hint.toLowerCase(Locale.ROOT);
        for (String sel : selectors) {
            if (sel != null && sel.equalsIgnoreCase(h)) {
                return true;
            }
        }
        return false;
    }

    /**
     * When the turbine builder is on Website pattern with a valid seed, rewrite the seed's zone /
     * rod-column coil ids (and layer count) from the current builder selection so the EditBox stays in sync.
     * Distinct rod coil → {@code mdct2}; same as zone → {@code mdct1}.
     */
    public static void syncTurbineWebsiteSeedFromCoilSelection(TurbineBuilderBlockEntity builder) {
        if (builder == null || builder.getRodPattern() != TurbineRodPatternLogic.PATTERN_WEBSITE) {
            return;
        }
        Optional<ModDecodedTurbineDesign> parsed = ModDecodedTurbineDesign.decode(builder.getWebsiteDesign());
        if (parsed.isEmpty()) {
            return;
        }
        String coilId = websiteCoilIdForOptionIndex(builder.getSelectedCoilIndex());
        String rodCoilId = websiteCoilIdForOptionIndex(builder.getSelectedRodCoilIndex());
        if (rodCoilId.equals(coilId)) {
            rodCoilId = null;
        }
        ModDecodedTurbineDesign updated = parsed.get()
                .withCoilId(coilId)
                .withRodCoilId(rodCoilId)
                .withCoilLayers(builder.getCoilLayerCount());
        builder.setWebsiteDesign(updated.code());
    }

    private static boolean selectorsContainHint(List<String> selectors, String hint) {
        if (selectors == null || hint == null || hint.isEmpty()) {
            return false;
        }
        String h = hint.toLowerCase(Locale.ROOT);
        for (String sel : selectors) {
            if (sel == null) {
                continue;
            }
            String s = sel.toLowerCase(Locale.ROOT);
            if (s.equals(h) || s.endsWith("/" + h) || s.endsWith(":" + h) || s.contains(h)) {
                // Prefer path-tail matches; "contains" is a fallback for tags like copper_all / ender_molten.
                String path = s.contains(":") ? s.substring(s.indexOf(':') + 1) : s;
                if (path.equals(h) || path.endsWith("/" + h) || path.contains(h)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int preferredHeatSinkOption(ModDecodedReactorDesign design) {
        Map<String, Integer> counts = new HashMap<>();
        for (String cell : design.cells()) {
            if (cell == null || "rod".equals(cell) || "air".equals(cell)) {
                continue;
            }
            counts.merge(cell, 1, Integer::sum);
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        if (best == null) {
            return 0;
        }
        int idx = findHeatSinkOptionIndex(best);
        return idx >= 0 ? idx : 0;
    }

    private static void applyReactorFuel(ReactorBuilderBlockEntity builder, String fuelId) {
        List<Identifier> ids = FuelLoader.getVisibleFuelIds();
        if (ids.isEmpty() || fuelId == null) {
            return;
        }
        Identifier want = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, fuelId);
        for (int i = 0; i < ids.size(); i++) {
            if (want.equals(ids.get(i))) {
                builder.setSimFuelIndex(i);
                return;
            }
        }
    }

    private static void applyReactorCoolant(ReactorBuilderBlockEntity builder, String coolant) {
        if ("water".equals(coolant)) {
            List<Identifier> ids = CoolantLoader.getVisibleCoolantIds();
            for (int i = 0; i < ids.size(); i++) {
                if (CoolantLoader.WATER_COOLANT_ID.equals(ids.get(i))) {
                    // simCoolantIndex: 0 = None, then visible coolants
                    builder.setSimCoolantIndex(1 + i);
                    return;
                }
            }
            builder.setSimCoolantIndex(1);
        } else {
            builder.setSimCoolantIndex(0);
        }
    }
}
