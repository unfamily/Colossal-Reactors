package net.unfamily.colossal_reactors.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;

/** Menu for turbine controller GUI stats sync. */
public class TurbineControllerMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    public TurbineControllerMenu(int containerId, Inventory playerInventory, TurbineControllerBlockEntity be) {
        super(ModMenuTypes.TURBINE_CONTROLLER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                var r = be.getCachedResult();
                return switch (index) {
                    case 0 -> r.valid() ? 1 : 0;
                    case 1 -> r.validBladeCount();
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, be.getLastRfPerTick());
                    case 3 -> (int) Math.min(Integer.MAX_VALUE, be.getLastSteamPerTick());
                    case 4 -> (int) (be.getLastCoilEff() * 1000);
                    case 5 -> (int) (be.getLastBladeEff() * 1000);
                    case 6 -> {
                        var failure = r.failure();
                        yield failure == null ? -1 : failure.ordinal();
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return 7;
            }
        };
        addDataSlots(data);
    }

    public TurbineControllerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.TURBINE_CONTROLLER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new SimpleContainerData(7);
        addDataSlots(data);
    }

    public boolean isValid() { return data.get(0) != 0; }
    public int getBladeCount() { return data.get(1); }
    public long getRfPerTick() { return data.get(2) & 0xFFFFFFFFL; }
    public int getSteamPerTick() { return data.get(3); }
    public double getCoilEff() { return data.get(4) / 1000.0; }
    public double getBladeEff() { return data.get(5) / 1000.0; }

    /** {@link net.unfamily.colossal_reactors.turbine.TurbineValidation.FailureCode#ordinal()}, or -1 if valid. */
    public int getFailureOrdinal() { return data.get(6); }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.TURBINE_CONTROLLER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
