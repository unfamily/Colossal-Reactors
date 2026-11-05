package net.unfamily.colossal_reactors.util;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Persists the player who placed or first used a craftable machine; used for recipe stage gates.
 */
public final class BlockCraftOwner {
    public static final String NBT_OWNER = "ownerPlayer";

    private BlockCraftOwner() {}

    @Nullable
    public static UUID read(CompoundTag tag) {
        if (tag.hasUUID(NBT_OWNER)) {
            return tag.getUUID(NBT_OWNER);
        }
        return null;
    }

    public static void write(CompoundTag tag, @Nullable UUID owner) {
        if (owner != null) {
            tag.putUUID(NBT_OWNER, owner);
        } else {
            tag.remove(NBT_OWNER);
        }
    }

    public static void claimOnFirstOpen(BlockEntity be, @Nullable ServerPlayer player, @Nullable UUID currentOwner) {
        if (be == null || player == null || currentOwner != null) {
            return;
        }
        if (be instanceof BlockCraftOwnerHolder holder) {
            holder.setOwnerPlayerUuid(player.getUUID());
            be.setChanged();
        }
    }

    /** Claim from block interaction when the BE implements {@link BlockCraftOwnerHolder}. */
    public static void claimOnUse(BlockEntity be, @Nullable Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(be instanceof BlockCraftOwnerHolder holder)) {
            return;
        }
        claimOnFirstOpen(be, serverPlayer, holder.getOwnerPlayerUuid());
    }

    /** Claim when a player places the block ({@code setPlacedBy}). */
    public static void claimOnPlace(BlockEntity be, @Nullable LivingEntity placer) {
        if (!(placer instanceof ServerPlayer serverPlayer) || !(be instanceof BlockCraftOwnerHolder holder)) {
            return;
        }
        claimOnFirstOpen(be, serverPlayer, holder.getOwnerPlayerUuid());
    }

    @Nullable
    public static ServerPlayer resolve(@Nullable Level level, @Nullable UUID ownerUuid) {
        if (level == null || ownerUuid == null || level.isClientSide()) {
            return null;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public interface BlockCraftOwnerHolder {
        @Nullable
        UUID getOwnerPlayerUuid();

        void setOwnerPlayerUuid(@Nullable UUID uuid);
    }
}
