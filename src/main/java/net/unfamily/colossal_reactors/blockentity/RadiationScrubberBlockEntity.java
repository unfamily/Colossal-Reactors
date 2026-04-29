package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.colossal_reactors.transfer.LegacyEnergyStorageEnergyHandler;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.menu.RadiationScrubberMenu;
import net.unfamily.colossal_reactors.radiation_scrubber.RadiationScrubberCatalystsLoader;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Radiation Scrubber: removes Mekanism radiation in area via IRadiationManager (reflection).
 * Requires energy (config). Slot 0 = optional catalyst (datapack; when present: bonus radius/effectiveness, one consumed per scrub); Slot 1 = general. Without catalyst still consumes energy and removes radiation at base rate.
 */
public class RadiationScrubberBlockEntity extends BlockEntity implements MenuProvider {

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_ENERGY = "Energy";
    /** Capacity for Mekanism chemical (gas) tank in mb. Same position as melter tank in GUI. */
    private static final long CHEMICAL_TANK_CAPACITY = 10_000L;

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
    private final LegacyEnergyStorageEnergyHandler energyHandlerCapability;
    private ResourceHandler<ItemResource> itemResourceCapability;

    /** Lazy-created Mekanism chemical tank (IChemicalTank/IChemicalHandler). Only non-null when Mekanism loaded. */
    private Object chemicalTank;

    /** ContainerData: 0-2 pos, 3-4 energy, 5-6 tank amount/cap, 7 = gas type string length, 8-23 = packed chars (4 per int). */
    private static final int DATA_GAS_TYPE_LENGTH_INDEX = 7;
    private static final int DATA_GAS_TYPE_START = 8;
    private static final int DATA_GAS_TYPE_INTS = 16;
    private static final int DATA_TOTAL = DATA_GAS_TYPE_START + DATA_GAS_TYPE_INTS;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> worldPosition.getX();
                case 1 -> worldPosition.getY();
                case 2 -> worldPosition.getZ();
                case 3 -> energyStorage.getEnergyStored();
                case 4 -> energyStorage.getMaxEnergyStored();
                case 5 -> getChemicalTankAmount();
                case 6 -> getChemicalTankCapacity();
                default -> {
                    if (index == DATA_GAS_TYPE_LENGTH_INDEX) {
                        String name = getChemicalTypeRegistryName();
                        yield name != null ? name.length() : 0;
                    }
                    if (index >= DATA_GAS_TYPE_START && index < DATA_TOTAL) {
                        String name = getChemicalTypeRegistryName();
                        if (name == null || name.isEmpty()) yield 0;
                        int base = (index - DATA_GAS_TYPE_START) * 4;
                        int v = 0;
                        for (int i = 0; i < 4 && base + i < name.length(); i++)
                            v |= (name.charAt(base + i) & 0xFF) << (i * 8);
                        yield v;
                    }
                    yield 0;
                }
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 3) energyStorage.setEnergy(Math.min(value, energyStorage.getMaxEnergyStored()));
        }

        @Override
        public int getCount() {
            return DATA_TOTAL;
        }
    };

    public RadiationScrubberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIATION_SCRUBBER_BE.get(), pos, state);
        int capacity = Config.RADIATION_SCRUBBER_ENERGY_CAPACITY.get();
        this.energyStorage = new RadiationScrubberEnergyStorage(capacity);
        this.energyHandlerCapability = new LegacyEnergyStorageEnergyHandler(energyStorage);
    }

    /** Returns the Mekanism chemical handler: tank that accepts only radioactive gas (isolated storage). Caller may cast to IChemicalHandler. */
    public Object getChemicalHandler() {
        if (chemicalTank == null) {
            Object tank = createChemicalTankReflection();
            if (tank != null) {
                chemicalTank = wrapRadioactiveGasOnlyHandler(tank);
            }
        }
        return chemicalTank;
    }

    /** Creates a tank that accepts only radioactive chemicals (same approach as Mekanism Radioactive Waste Barrel: ChemicalAttributeValidator). Then wrapper restricts to gas only. */
    private static Object createChemicalTankReflection() {
        try {
            Class<?> tankClass = Class.forName("mekanism.api.chemical.BasicChemicalTank");
            Class<?> listenerClass = Class.forName("mekanism.api.IContentsListener");
            Class<?> validatorInterface = Class.forName("mekanism.api.chemical.attribute.ChemicalAttributeValidator");
            String radioactivityClassName = "mekanism.api.datamaps.chemical.attribute.ChemicalRadioactivity";
            InvocationHandler validatorHandler = (proxy, method, args) -> {
                String name = method.getName();
                if ("validate".equals(name) && args != null && args.length > 0) {
                    Object attr = args[0];
                    return attr != null && radioactivityClassName.equals(attr.getClass().getName());
                }
                if ("process".equals(name) && args != null && args.length > 0) {
                    Object arg = args[0];
                    if (arg == null) return false;
                    try {
                        Object chemical = arg;
                        try {
                            Method getChemical = arg.getClass().getMethod("getChemical");
                            chemical = getChemical.invoke(arg);
                        } catch (NoSuchMethodException ignored) {
                            try {
                                Method value = arg.getClass().getMethod("value");
                                chemical = value.invoke(arg);
                            } catch (NoSuchMethodException ignored2) {
                                // arg is Chemical itself
                            }
                        }
                        if (chemical == null) return false;
                        return Boolean.TRUE.equals(chemical.getClass().getMethod("isRadioactive").invoke(chemical));
                    } catch (Throwable e) {
                        return false;
                    }
                }
                return null;
            };
            Object attributeValidator = Proxy.newProxyInstance(validatorInterface.getClassLoader(), new Class<?>[]{validatorInterface}, validatorHandler);
            Method create = tankClass.getMethod("createWithValidator", long.class, validatorInterface, listenerClass);
            return create.invoke(null, CHEMICAL_TANK_CAPACITY, attributeValidator, null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Wraps the tank so only radioactive gas is accepted (reject non-gas or non-radioactive). Storage is isolated (no irradiation). */
    private static Object wrapRadioactiveGasOnlyHandler(Object innerTank) {
        try {
            Class<?> handlerClass = Class.forName("mekanism.api.chemical.IChemicalHandler");
            InvocationHandler h = (proxy, method, args) -> {
                if ("insertChemical".equals(method.getName()) && args != null && args.length >= 1) {
                    Object stack = args.length >= 2 ? args[1] : args[0];
                    if (stack != null && !chemicalStackEmpty(stack) && !isRadioactiveGas(stack)) {
                        return stack; // reject: only radioactive gas allowed
                    }
                }
                return method.invoke(innerTank, args);
            };
            return Proxy.newProxyInstance(handlerClass.getClassLoader(), new Class<?>[]{handlerClass}, h);
        } catch (Throwable ignored) {
            return innerTank;
        }
    }

    private static boolean chemicalStackEmpty(Object stack) {
        try {
            long amount = ((Number) stack.getClass().getMethod("getAmount").invoke(stack)).longValue();
            return amount <= 0;
        } catch (Throwable e) {
            return true;
        }
    }

    private static boolean isRadioactiveGas(Object stack) {
        try {
            Boolean radioactive = (Boolean) stack.getClass().getMethod("isRadioactive").invoke(stack);
            if (!Boolean.TRUE.equals(radioactive)) return false;
            Object chemical = stack.getClass().getMethod("getChemical").invoke(stack);
            if (chemical == null) return false;
            Boolean gaseous = (Boolean) chemical.getClass().getMethod("isGaseous").invoke(chemical);
            return Boolean.TRUE.equals(gaseous);
        } catch (Throwable e) {
            return false;
        }
    }

    private int getChemicalTankAmount() {
        if (chemicalTank == null) return 0;
        try {
            Object stack = chemicalTank.getClass().getMethod("getChemicalInTank", int.class).invoke(chemicalTank, 0);
            if (stack == null) return 0;
            long amount = ((Number) stack.getClass().getMethod("getAmount").invoke(stack)).longValue();
            return (int) Math.min(amount, Integer.MAX_VALUE);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private int getChemicalTankCapacity() {
        if (chemicalTank == null) return 0;
        try {
            long cap = ((Number) chemicalTank.getClass().getMethod("getChemicalTankCapacity", int.class).invoke(chemicalTank, 0)).longValue();
            return (int) Math.min(cap, Integer.MAX_VALUE);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** Returns the gas type registry name (e.g. "mekanism:uranium_hexafluoride") for sync to client, or null if empty. */
    @Nullable
    public String getChemicalTypeRegistryName() {
        Object handler = getChemicalHandler();
        if (handler == null) return null;
        try {
            Object stack = handler.getClass().getMethod("getChemicalInTank", int.class).invoke(handler, 0);
            if (stack == null) return null;
            if (Boolean.TRUE.equals(stack.getClass().getMethod("isEmpty").invoke(stack))) return null;
            Object rl = stack.getClass().getMethod("getTypeRegistryName").invoke(stack);
            return rl != null ? rl.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Energy (RF) per tick for gas destruction. With dual-process multiplier applied when zone is radioactive. */
    private static final int BASE_ENERGY_PER_GAS_TICK = 100;
    /** Multiplier for energy and catalyst when scrubber runs both processes (scrub area + destroy gas). */
    private static final int DUAL_PROCESS_MULTIPLIER = 2;

    /**
     * When the scrubber block is broken, dump radiation at the position (like Mekanism's Radioactive Waste Barrel).
     * Call from block's onRemove before super.
     */
    public static void dumpRadiationOnBreak(Level level, BlockPos pos, RadiationScrubberBlockEntity scrubber) {
        Object handler = scrubber.getChemicalHandler();
        if (handler == null) return;
        try {
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Class<?> chemicalHandlerClass = Class.forName("mekanism.api.chemical.IChemicalHandler");
            Object manager = managerClass.getField("INSTANCE").get(null);
            managerClass.getMethod("dumpRadiation", Level.class, BlockPos.class, chemicalHandlerClass, boolean.class)
                    .invoke(manager, level, pos, handler, true);
        } catch (Throwable ignored) {
            // Mekanism API or handler not IChemicalHandler
        }
    }

    /** Destroys gas from tank: BASE_ENERGY_PER_GAS_TICK * multiplier RF per tick, destroys up to (config base * gasMult) mB. Isolated storage: no radiation released. */
    private void destroyGasFromTank(int energyPerGasTick) {
        Object handler = getChemicalHandler();
        if (handler == null) return;
        int amount = getChemicalTankAmount();
        if (amount <= 0) return;
        if (energyStorage.getEnergyStored() < energyPerGasTick) return;
        ItemStack catalystStack = itemHandler.getStackInSlot(0);
        boolean hasCatalyst = !catalystStack.isEmpty() && isCatalyst(catalystStack);
        int gasMult = hasCatalyst ? RadiationScrubberCatalystsLoader.getGasDestroyMult() : 1;
        int base = Config.RADIATION_SCRUBBER_BASE_GAS_DESTRUCTION.get();
        long toDestroy = Math.min((long) base * gasMult, amount);
        if (toDestroy <= 0) return;
        try {
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object execute = actionClass.getEnumConstants()[0]; // EXECUTE is first enum
            for (Object c : actionClass.getEnumConstants()) {
                if ("EXECUTE".equals(c.toString())) { execute = c; break; }
            }
            handler.getClass().getMethod("extractChemical", int.class, long.class, actionClass).invoke(handler, 0, toDestroy, execute);
            energyStorage.extractEnergy(energyPerGasTick, false);
        } catch (Throwable ignored) {
            // Mekanism API
        }
    }

    /** True if the stack matches any catalyst entry from datapack. Entries with # are item tags, otherwise item ids. */
    public static boolean isCatalyst(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (String entry : RadiationScrubberCatalystsLoader.getCatalysts()) {
            if (entry == null || entry.isBlank()) continue;
            try {
                if (entry.startsWith("#")) {
                    Identifier tagId = Identifier.parse(entry.substring(1).trim());
                    TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
                    if (stack.is(tag)) return true;
                } else {
                    Identifier itemId = Identifier.parse(entry.trim());
                    Item item = BuiltInRegistries.ITEM.getValue(itemId);
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
        int radiusBlocks = Config.RADIATION_SCRUBBER_RADIUS_BLOCKS.get();
        boolean hasGas = getChemicalTankAmount() > 0;
        boolean hasRadiation = hasRadiationInArea(level, pos, radiusBlocks);
        // No work: consume nothing
        if (!hasGas && !hasRadiation) return;
        // 2x only when doing both jobs (remove gas + scrub area)
        int multiplier = (hasGas && hasRadiation) ? DUAL_PROCESS_MULTIPLIER : 1;
        int energyPerGasTick = BASE_ENERGY_PER_GAS_TICK * multiplier;
        if (hasGas) {
            destroyGasFromTank(energyPerGasTick);
        }
        int interval = Config.RADIATION_SCRUBBER_INTERVAL_TICKS.get();
        if (level.getGameTime() % interval != 0) return;
        // Per-interval: catalyst and (if scrubbing) energy for scrub
        int catalystConsume = (hasRadiation ? multiplier : 0) + (hasGas && !hasRadiation ? 1 : 0);
        if (catalystConsume <= 0) return;
        ItemStack catalystStack = itemHandler.getStackInSlot(0);
        boolean hasCatalyst = !catalystStack.isEmpty() && isCatalyst(catalystStack) && catalystStack.getCount() >= catalystConsume;
        if (hasRadiation) {
            int energyPerTick = Config.RADIATION_SCRUBBER_ENERGY_PER_TICK.get() * multiplier;
            if (energyStorage.getEnergyStored() < energyPerTick) return;
            int decayPerTick = Config.RADIATION_SCRUBBER_BASE_RADIATION_REMOVAL.get();
            if (hasCatalyst) {
                decayPerTick *= RadiationScrubberCatalystsLoader.getEffectiveness();
                catalystStack.shrink(catalystConsume);
            } else {
                return;
            }
            energyStorage.extractEnergy(energyPerTick, false);
            scrubRadiationInArea(level, pos, radiusBlocks, decayPerTick);
        } else {
            // Only gas job: consume 1 catalyst per interval (energy already used in destroyGasFromTank)
            if (hasCatalyst) {
                catalystStack.shrink(catalystConsume);
            }
        }
    }

    /** Returns true if there is any radiation source in the scrubber's area (magnitude above minimum). */
    private boolean hasRadiationInArea(Level level, BlockPos center, int radiusBlocks) {
        try {
            Class<?> managerClass = Class.forName("mekanism.api.radiation.IRadiationManager");
            Object manager = managerClass.getField("INSTANCE").get(null);
            Boolean enabled = (Boolean) managerClass.getMethod("isRadiationEnabled").invoke(manager);
            if (!Boolean.TRUE.equals(enabled)) return false;
            double minMagnitude = ((Number) managerClass.getMethod("minRadiationMagnitude").invoke(manager)).doubleValue();
            long radiusSq = (long) radiusBlocks * radiusBlocks;
            int minChunkX = (center.getX() - radiusBlocks) >> 4;
            int maxChunkX = (center.getX() + radiusBlocks) >> 4;
            int minChunkZ = (center.getZ() - radiusBlocks) >> 4;
            int maxChunkZ = (center.getZ() + radiusBlocks) >> 4;
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
                        if (current > minMagnitude) return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Mekanism not present or API changed
        }
        return false;
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

    public EnergyHandler getEnergyHandlerForCapability() {
        return energyHandlerCapability;
    }

    public ResourceHandler<ItemResource> getItemResourceHandlerForCapability() {
        if (itemResourceCapability == null) {
            itemResourceCapability = LegacyItemHandlerResourceHandler.wrap(itemHandler);
        }
        return itemResourceCapability;
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        Level level = getLevel();
        if (level != null && !oldState.is(level.getBlockState(pos).getBlock())) {
            dumpRadiationOnBreak(level, pos, this);
            dropAllContents();
        }
        super.preRemoveSideEffects(pos, oldState);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output);
        output.putInt(TAG_ENERGY, energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input);
        input.getInt(TAG_ENERGY).ifPresent(energyStorage::setEnergy);
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
