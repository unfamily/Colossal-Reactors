package net.unfamily.colossal_reactors.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
/** Places {@link GasBlock} in the world (not a liquid block). */
public class GasBucketItem extends BucketItem {
    private final RegisteredGas gas;

    public GasBucketItem(Properties properties, RegisteredGas gas) {
        super(gas.sourceFluid(), properties);
        this.gas = gas;
    }

    public static ItemStack createFilledBucket(RegisteredGas gas) {
        return new ItemStack(gas.bucketItem());
    }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState clicked = level.getBlockState(pos);
        BlockPos placePos = clicked.canBeReplaced() ? pos : pos.relative(context.getClickedFace());

        if (tryPlaceGas(level, placePos)) {
            if (!level.isClientSide) {
                Player player = context.getPlayer();
                if (player != null && !player.getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                    if (!player.getInventory().add(new ItemStack(Items.BUCKET))) {
                        player.drop(new ItemStack(Items.BUCKET), false);
                    }
                }
                level.playSound(null, placePos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(context.getPlayer(), GameEvent.FLUID_PLACE, placePos);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private boolean tryPlaceGas(Level level, BlockPos pos) {
        if (pos.getY() >= level.getMaxBuildHeight() || pos.getY() < level.getMinBuildHeight()) {
            return false;
        }
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        return level.setBlock(pos, gas.block().defaultBlockState(), Block.UPDATE_ALL);
    }
}
