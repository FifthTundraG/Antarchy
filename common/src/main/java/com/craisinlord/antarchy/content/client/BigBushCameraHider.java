package com.craisinlord.antarchy.content.client;

import com.craisinlord.antarchy.content.block.BigBushBlock;
import com.craisinlord.antarchy.content.block.BigBushPartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public final class BigBushCameraHider {
    private static BlockPos hiddenOrigin;

    private BigBushCameraHider() {
    }

    public static void update(Minecraft mc) {
        BlockPos newOrigin = null;
        if (mc.level != null && mc.player != null) {
            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            BlockPos pos = BlockPos.containing(cameraPos);
            BlockState state = mc.level.getBlockState(pos);
            if (state.getBlock() instanceof BigBushBlock && state.getValue(BigBushBlock.AGE) > 0) {
                newOrigin = pos;
            } else if (state.getBlock() instanceof BigBushPartBlock) {
                newOrigin = BigBushPartBlock.findOrigin(mc.level, pos);
            }
        }
        if (!Objects.equals(newOrigin, hiddenOrigin)) {
            BlockPos previous = hiddenOrigin;
            hiddenOrigin = newOrigin;
            markDirty(mc, previous);
            markDirty(mc, newOrigin);
        }
    }

    private static void markDirty(Minecraft mc, BlockPos origin) {
        if (origin != null && mc.levelRenderer != null) {
            mc.levelRenderer.setBlocksDirty(
                    origin.getX() - 2, origin.getY() - 1, origin.getZ() - 2,
                    origin.getX() + 2, origin.getY() + 3, origin.getZ() + 2);
        }
    }

    public static boolean shouldFade(BlockPos pos) {
        BlockPos origin = hiddenOrigin;
        if (origin == null) {
            return false;
        }
        return Math.abs(pos.getX() - origin.getX()) <= 1
                && Math.abs(pos.getZ() - origin.getZ()) <= 1
                && pos.getY() >= origin.getY()
                && pos.getY() <= origin.getY() + 2;
    }
}
