package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.menu.RadiationScrubberMenu;
import net.unfamily.colossal_reactors.radiation_scrubber.RadiationScrubberCatalystsLoader;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Radiation Scrubber: removes Mekanism radiation in area via IRadiationManager (reflection).
 * Requires energy (config). Slot 0 = optional catalyst (datapack; when present: bonus radius/effectiveness, one consumed per scrub); Slot 1 = general. Without catalyst still consumes energy and removes radiation at base rate.
 */
public class RadiationScrubberBlockEntity extends BlockEntity implements MenuProvider {

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_ENERGY = "Energy";

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return !stack.isEmpty() && isCatalyst(stack);
            return super.isItemValid(slot, stack);
        }
    };

    private final RadiationScrubberEnergyStorage energyStorage;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> worldPosition.getX();
                case 1 -> worldPosition.getY();
                case 2 -> worldPosition.getZ();
                case 3 -> energyStorage.getEnergyStored();
                case 4 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 3) energyStorage.setEnergy(Math.min(value, energyStorage.getMaxEnergyStored()));
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public RadiationScrubberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIATION_SCRUBBER_BE.get(), pos, state);
        int capacity = Config.RADIATION_SCRUBBER_ENERGY_CAPACITY.get();
        this.energyStorage = new RadiationScrubberEnergyStorage(capacity);
    }

    /** True if the stack matches any catalyst entry from datapack. Entries with # are item tags, otherwise item ids. */
    public static boolean isCatalyst(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (String entry : RadiationScrubberCatalystsLoader.getCatalysts()) {
            if (entry == null || entry.isBlank()) continue;
            try {
                if (entry.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.parse(entry.substring(1).trim());
                    TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
                    if (stack.is(tag)) return true;
                } else {
                    ResourceLocation itemId = ResourceLocation.parse(entry.trim());
                    Item item = BuiltInRegistries.ITEM.get(itemId);
                    if (item != null && !item.equals(net.minecraft.world.item.Items.AIR) && stack.is(item)) return true;
                }
            } catch (Exception ignored) {
                // skip malformed entry
            }
        }
        return false;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadiationScrubberBlockEntity be) {
        if (level.isClientSide()) return;
        if (!ModList.get().isLoaded("mekanism")) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        int interval = Config.RADIATION_SCRUBBER_INTERVAL_TICKS.get();
        if (level.getGameTime() % interval != 0) return;

        int energyPerTick = Config.RADIATION_SCRUBBER_ENERGY_PER_TICK.get();
        if (energyStorage.getEnergyStored() < energyPerTick) return;

        ItemStack catalystStack = itemHandler.getStackInSlot(0);
        boolean hasCatalyst = !catalystStack.isEmpty() && isCatalyst(catalystStack) && catalystStack.getCount() >= 1;

        int radiusBlocks = Config.RADIATION_SCRUBBER_RADIUS_BLOCKS.get();
        int decayPerTick = Config.RADIATION_SCRUBBER_BASE_RADIATION_REMOVAL.get();
        if (hasCatalyst) {
            decayPerTick *= RadiationScrubberCatalystsLoader.getEffectiveness();
            catalystStack.shrink(1);
        }

        energyStorage.extractEnergy(energyPerTick, false);
        scrubRadiationInArea(level, pos, radiusBlocks, decayPerTick);
    }

    /** Scale from config*mult to Sv/h removal per source (Mekanism uses Sv/h for magnitude). */
    private static final double REMOVAL_SCALE_SVH = 0.001;

    @SuppressWarnings("unchecked")
    private void scrubRadiationInArea(Level level, BlockPos center, int radiusBlocks, int decayPerTick) {
        try {
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Object manager = managerClass.getField("INSTANCE").get(null);
            Boolean enabled = (Boolean) managerClass.getMethod("isRadiationEnabled").invoke(manager);
            if (!Boolean.TRUE.equals(enabled)) return;

            double removalPerSource = decayPerTick * REMOVAL_SCALE_SVH;
            if (removalPerSource <= 0) return;

            long radiusSq = (long) radiusBlocks * radiusBlocks;
            int minChunkX = (center.getX() - radiusBlocks) >> 4;
            int maxChunkX = (center.getX() + radiusBlocks) >> 4;
            int minChunkZ = (center.getZ() - radiusBlocks) >> 4;
            int maxChunkZ = (center.getZ() + radiusBlocks) >> 4;

            double minMagnitude = ((Number) managerClass.getMethod("minRadiationMagnitude").invoke(manager)).doubleValue();

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    List<?> sources = (List<?>) managerClass.getMethod("getRadiationSources", Level.class, int.class, int.class)
                            .invoke(manager, level, chunkX, chunkZ);
                    if (sources == null) continue;
                    for (Object src : sources) {
                        if (src == null) continue;
                        BlockPos srcPos = (BlockPos) src.getClass().getMethod("getPosition").invoke(src);
                        if (center.distSqr(srcPos) > radiusSq) continue;
                        double current = ((Number) src.getClass().getMethod("getMagnitude").invoke(src)).doubleValue();
                        if (current <= minMagnitude) continue;
                        double toRemove = Math.min(current - minMagnitude, removalPerSource);
                        if (toRemove > 0) src.getClass().getMethod("radiate", double.class).invoke(src, -toRemove);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Mekanism not present or API changed
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.radiation_scrubber");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new RadiationScrubberMenu(id, playerInventory, this, data);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ContainerData getData() {
        return data;
    }

    public void dropAllContents() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = getBlockPos();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_ITEMS, itemHandler.serializeNBT(registries));
        tag.putInt(TAG_ENERGY, energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ITEMS)) {
            itemHandler.deserializeNBT(registries, tag.getCompound(TAG_ITEMS));
        }
        if (tag.contains(TAG_ENERGY)) {
            energyStorage.setEnergy(tag.getInt(TAG_ENERGY));
        }
    }

    private static final class RadiationScrubberEnergyStorage extends EnergyStorage {
        RadiationScrubberEnergyStorage(int capacity) {
            super(capacity, capacity, capacity);
        }
        void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }
}
