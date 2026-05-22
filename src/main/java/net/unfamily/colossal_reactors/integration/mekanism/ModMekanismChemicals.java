package net.unfamily.colossal_reactors.integration.mekanism;

import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import mekanism.common.registration.impl.ChemicalDeferredRegister;
import mekanism.common.registration.impl.DeferredChemical;
import net.unfamily.colossal_reactors.ColossalReactors;

/** Dirty/clean slurries for boron, chromium, and uranium (Mekanism dissolution chain). */
public final class ModMekanismChemicals {

    public static final ChemicalDeferredRegister CHEMICALS =
            new ChemicalDeferredRegister(ColossalReactors.MODID);

    private static int darken(int rgb, double factor) {
        int r = (int) (((rgb >> 16) & 0xFF) * factor);
        int g = (int) (((rgb >> 8) & 0xFF) * factor);
        int b = (int) ((rgb & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    public static final DeferredChemical<Chemical> CLEAN_BORON = CHEMICALS.register("clean_boron",
            () -> new Chemical(ChemicalBuilder.cleanSlurry().tint(0x3C5966)));
    public static final DeferredChemical<Chemical> DIRTY_BORON = CHEMICALS.register("dirty_boron",
            () -> new Chemical(ChemicalBuilder.dirtySlurry().tint(darken(0x3C5966, 0.55))));

    public static final DeferredChemical<Chemical> CLEAN_CHROMIUM = CHEMICALS.register("clean_chromium",
            () -> new Chemical(ChemicalBuilder.cleanSlurry().tint(0xCF8EAE)));
    public static final DeferredChemical<Chemical> DIRTY_CHROMIUM = CHEMICALS.register("dirty_chromium",
            () -> new Chemical(ChemicalBuilder.dirtySlurry().tint(darken(0xCF8EAE, 0.55))));

    public static final DeferredChemical<Chemical> CLEAN_URANIUM = CHEMICALS.register("clean_uranium",
            () -> new Chemical(ChemicalBuilder.cleanSlurry().tint(0x46664F)));
    public static final DeferredChemical<Chemical> DIRTY_URANIUM = CHEMICALS.register("dirty_uranium",
            () -> new Chemical(ChemicalBuilder.dirtySlurry().tint(darken(0x46664F, 0.55))));

    private ModMekanismChemicals() {}
}
