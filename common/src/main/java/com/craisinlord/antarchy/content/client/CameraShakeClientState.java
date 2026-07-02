package com.craisinlord.antarchy.content.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class CameraShakeClientState {
    public static final double TORETERROR_RANGE = 48.0D;
    public static final float TORETERROR_STRENGTH = 1.0F;
    public static final double NIGHTMARE_RANGE = 32.0D;
    public static final float NIGHTMARE_STRENGTH = 1.35F;

    private record Source(double range, float strength, BooleanSupplier active) {
    }

    private static final Map<LivingEntity, Source> SOURCES = new HashMap<>();

    private CameraShakeClientState() {
    }

    public static void register(LivingEntity entity, double range, float strength, BooleanSupplier active) {
        SOURCES.putIfAbsent(entity, new Source(range, strength, active));
    }

    public static float getStrength(Vec3 cameraPos) {
        if (SOURCES.isEmpty()) {
            return 0.0F;
        }

        float total = 0.0F;
        Iterator<Map.Entry<LivingEntity, Source>> iterator = SOURCES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LivingEntity, Source> entry = iterator.next();
            LivingEntity entity = entry.getKey();
            Source source = entry.getValue();
            if (entity.isRemoved() || !entity.isAlive() || !source.active().getAsBoolean()) {
                iterator.remove();
                continue;
            }

            double distance = Math.sqrt(cameraPos.distanceToSqr(entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D)));
            if (distance > source.range()) {
                continue;
            }

            total += (float) ((1.0D - distance / source.range()) * source.strength());
        }

        return total;
    }

    public static void clear() {
        SOURCES.clear();
    }
}
