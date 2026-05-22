package net.unfamily.colossal_reactors.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Block tags for reactor/turbine multiblock shell validation and builder frame placement.
 */
public final class ModBlockTags {

    public static final TagKey<Block> TURBINE_BLOCKS = block("turbine_blocks");
    public static final TagKey<Block> TURBINE_SHELL = block("turbine_blocks/shell");
    public static final TagKey<Block> TURBINE_SHELL_CASINGS = block("turbine_blocks/shell/casings");
    public static final TagKey<Block> TURBINE_SHELL_GLASSES = block("turbine_blocks/shell/glasses");
    public static final TagKey<Block> TURBINE_SHELL_PORTS = block("turbine_blocks/shell/ports");

    public static final TagKey<Block> REACTOR_BLOCKS = block("reactor_blocks");
    public static final TagKey<Block> REACTOR_SHELL = block("reactor_blocks/shell");
    public static final TagKey<Block> REACTOR_SHELL_CASINGS = block("reactor_blocks/shell/casings");
    public static final TagKey<Block> REACTOR_SHELL_GLASSES = block("reactor_blocks/shell/glasses");
    public static final TagKey<Block> REACTOR_SHELL_PORTS = block("reactor_blocks/shell/ports");

    private ModBlockTags() {}

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ColossalReactors.MODID, path));
    }
}
