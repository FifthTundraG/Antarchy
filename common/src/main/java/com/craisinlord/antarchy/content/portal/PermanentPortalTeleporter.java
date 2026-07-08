package com.craisinlord.antarchy.content.portal;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PermanentPortalTeleporter {
    private static final int SEARCH_RADIUS = 8;
    private static final int VERTICAL_SEARCH = 24;

    private PermanentPortalTeleporter() {
    }

    public static void teleport(Entity entity, PermanentPortalType type) {
        if (!(entity.level() instanceof ServerLevel sourceLevel) || !entity.isAlive()) {
            return;
        }

        ServerLevel destination = resolveDestination(sourceLevel, type);
        if (destination == null) {
            return;
        }

        Vec3 arrival = findArrivalPosition(entity, destination, entity.blockPosition(), type.platformBlock().defaultBlockState());
        Entity movedEntity = moveEntity(entity, destination, arrival);
        if (movedEntity != null) {
            movedEntity.setPortalCooldown();
        }
    }

    @Nullable
    private static ServerLevel resolveDestination(ServerLevel sourceLevel, PermanentPortalType type) {
        if (sourceLevel.dimension() == type.primaryDimension()) {
            return sourceLevel.getServer().overworld();
        }
        return sourceLevel.getServer().getLevel(type.primaryDimension());
    }

    private static Vec3 findArrivalPosition(Entity entity, ServerLevel destination, BlockPos preferredPos, BlockState platformState) {
        Vec3 safe = findSafeArrivalPosition(entity, destination, preferredPos);
        return safe != null ? safe : createFallbackPlatform(destination, preferredPos, platformState);
    }

    @Nullable
    private static Vec3 findSafeArrivalPosition(Entity entity, ServerLevel destination, BlockPos preferredPos) {
        Set<BlockPos> candidates = new LinkedHashSet<>();
        addCandidate(candidates, preferredPos);

        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int xOff = -radius; xOff <= radius; xOff++) {
                for (int zOff = -radius; zOff <= radius; zOff++) {
                    if (radius > 0 && Math.abs(xOff) != radius && Math.abs(zOff) != radius) {
                        continue;
                    }

                    BlockPos horizontalPos = preferredPos.offset(xOff, 0, zOff);
                    addCandidate(candidates, destination.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, horizontalPos));
                    addCandidate(candidates, destination.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, horizontalPos));
                    for (int yOff = 0; yOff <= VERTICAL_SEARCH; yOff++) {
                        addCandidate(candidates, horizontalPos.above(yOff));
                        if (yOff > 0) {
                            addCandidate(candidates, horizontalPos.below(yOff));
                        }
                    }
                }
            }
        }

        for (BlockPos candidate : candidates) {
            Vec3 safe = tryFindSafePosition(entity, destination, candidate);
            if (safe != null) {
                return safe;
            }
        }

        return null;
    }

    private static void addCandidate(Set<BlockPos> candidates, @Nullable BlockPos pos) {
        if (pos == null) {
            return;
        }

        candidates.add(pos);
        candidates.add(pos.above());
        if (pos.getY() > Integer.MIN_VALUE) {
            candidates.add(pos.below());
        }
    }

    @Nullable
    private static Vec3 tryFindSafePosition(Entity entity, ServerLevel destination, BlockPos candidate) {
        Vec3 safe = DismountHelper.findSafeDismountLocation(entity.getType(), destination, candidate, true);
        if (safe != null) {
            return safe;
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            safe = DismountHelper.findSafeDismountLocation(entity.getType(), destination, candidate.relative(direction), true);
            if (safe != null) {
                return safe;
            }
        }

        return null;
    }

    private static Vec3 createFallbackPlatform(ServerLevel destination, BlockPos preferredPos, BlockState platformState) {
        int minY = destination.getMinBuildHeight() + 2;
        int maxY = destination.getMaxBuildHeight() - 4;
        BlockPos center = new BlockPos(preferredPos.getX(), Mth.clamp(preferredPos.getY(), minY, maxY), preferredPos.getZ());

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                destination.setBlock(center.offset(dx, -1, dz), platformState, Block.UPDATE_ALL);
                destination.removeBlock(center.offset(dx, 0, dz), false);
                destination.removeBlock(center.offset(dx, 1, dz), false);
            }
        }

        return Vec3.atBottomCenterOf(center);
    }

    @Nullable
    private static Entity moveEntity(Entity entity, ServerLevel destination, Vec3 destinationPos) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(destination, destinationPos.x, destinationPos.y, destinationPos.z, player.getYRot(), player.getXRot());
            return player;
        }

        if (entity.level() == destination) {
            entity.teleportTo(destinationPos.x, destinationPos.y, destinationPos.z);
            return entity;
        }

        return entity.changeDimension(new DimensionTransition(destination, destinationPos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), DimensionTransition.DO_NOTHING));
    }
}
