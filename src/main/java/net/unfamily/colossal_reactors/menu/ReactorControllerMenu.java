package net.unfamily.colossal_reactors.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.unfamily.colossal_reactors.block.ControllerState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.ReactorControllerBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;

/**
 * Menu for Reactor OS GUI when controller is ON. Syncs reactor stats and state for the dark panel.
 */
public class ReactorControllerMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    private static final int INDEX_STATE = 0;
    private static final int INDEX_ROD_COUNT = 1;
    private static final int INDEX_ROD_COLUMNS = 2;
    private static final int INDEX_COOLANT = 3;
    private static final int INDEX_HAS_FUEL = 4;
    private static final int INDEX_ENERGY_PER_TICK = 5;
    private static final int INDEX_STEAM_PER_TICK = 6;
    private static final int INDEX_WATER_PER_TICK = 7;
    private static final int INDEX_FUEL_PER_TICK_HUNDREDTHS = 8;
    private static final int INDEX_POS_X = 9;
    private static final int INDEX_POS_Y = 10;
    private static final int INDEX_POS_Z = 11;
    private static final int INDEX_HAS_REDSTONE_PORT = 12;
    private static final int INDEX_REDSTONE_GATE_SATISFIED = 13;
    private static final int DATA_COUNT = 14;

    public ReactorControllerMenu(int containerId, Inventory playerInventory, ReactorControllerBlockEntity blockEntity) {
        super(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                var result = blockEntity.getCachedResult();
                var state = blockEntity.getLevel() != null
                        ? blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getValue(ReactorControllerBlock.STATE)
                        : ControllerState.OFF;
                return switch (index) {
                    case INDEX_STATE -> state.ordinal();
                    case INDEX_ROD_COUNT -> result != null ? result.rodCount() : 0;
                    case INDEX_ROD_COLUMNS -> result != null ? result.rodColumns() : 0;
                    case INDEX_COOLANT -> result != null ? result.coolantCount() : 0;
                    case INDEX_HAS_FUEL -> result != null && blockEntity.getLevel() != null
                            ? (rodsHaveFuel(blockEntity.getLevel(), result) ? 1 : 0) : 0;
                    case INDEX_ENERGY_PER_TICK -> blockEntity.getLastRfPerTick();
                    case INDEX_STEAM_PER_TICK -> blockEntity.getLastSteamPerTick();
                    case INDEX_WATER_PER_TICK -> blockEntity.getLastWaterPerTick();
                    case INDEX_FUEL_PER_TICK_HUNDREDTHS -> blockEntity.getLastFuelPerTickHundredths();
                    case INDEX_POS_X -> blockEntity.getBlockPos().getX();
                    case INDEX_POS_Y -> blockEntity.getBlockPos().getY();
                    case INDEX_POS_Z -> blockEntity.getBlockPos().getZ();
                    case INDEX_HAS_REDSTONE_PORT -> result != null && blockEntity.getLevel() != null
                            ? (hasRedstonePortInResult(blockEntity.getLevel(), result) ? 1 : 0) : 0;
                    case INDEX_REDSTONE_GATE_SATISFIED -> result != null && blockEntity.getLevel() instanceof ServerLevel sl
                            ? (ReactorControllerBlock.isRedstoneGateSatisfied(sl, result) ? 1 : 0) : 1;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(data);
    }

    private static boolean rodsHaveFuel(net.minecraft.world.level.Level level, ReactorValidation.Result result) {
        for (int x = result.minX(); x <= result.maxX(); x++) {
            for (int y = result.minY(); y <= result.maxY(); y++) {
                for (int z = result.minZ(); z <= result.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.REACTOR_ROD.get())
                            && level.getBlockEntity(pos) instanceof ReactorRodBlockEntity rod
                            && rod.getTotalFuelUnits() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasRedstonePortInResult(net.minecraft.world.level.Level level, ReactorValidation.Result result) {
        for (int x = result.minX(); x <= result.maxX(); x++) {
            for (int y = result.minY(); y <= result.maxY(); y++) {
                for (int z = result.minZ(); z <= result.maxZ(); z++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.REDSTONE_PORT.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ReactorControllerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
    }

    public int getControllerStateId() { return data.get(INDEX_STATE); }
    public int getRodCount() { return data.get(INDEX_ROD_COUNT); }
    public int getRodColumns() { return data.get(INDEX_ROD_COLUMNS); }
    public int getCoolantCount() { return data.get(INDEX_COOLANT); }
    public boolean hasFuel() { return data.get(INDEX_HAS_FUEL) != 0; }
    public int getEnergyPerTick() { return data.get(INDEX_ENERGY_PER_TICK); }
    public int getSteamPerTick() { return data.get(INDEX_STEAM_PER_TICK); }
    public int getWaterPerTick() { return data.get(INDEX_WATER_PER_TICK); }
    /** Fuel consumption in fuel units/tick as hundredths (e.g. 26 = 0.26). */
    public int getFuelPerTickHundredths() { return data.get(INDEX_FUEL_PER_TICK_HUNDREDTHS); }

    /** Synced block pos for refresh button (client). */
    public BlockPos getControllerBlockPos() {
        return new BlockPos(data.get(INDEX_POS_X), data.get(INDEX_POS_Y), data.get(INDEX_POS_Z));
    }

    /** True if the reactor multiblock contains at least one redstone port. */
    public boolean hasRedstonePort() {
        return data.get(INDEX_HAS_REDSTONE_PORT) != 0;
    }

    /** True when the reactor can run this tick (no redstone ports, or at least one port active). */
    public boolean isRedstoneGateSatisfied() {
        return data.get(INDEX_REDSTONE_GATE_SATISFIED) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.REACTOR_CONTROLLER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
