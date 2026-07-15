package com.craisinlord.antarchy.content.entity.glimmer;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.content.entity.multipart.MultipartEntityOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class GlimmerFrogBehavior implements GlimmerVariantBehavior {
    private static final int SMALL_SLIME_SIZE = 1;
    private static final TagKey<EntityType<?>> NO_EAT_TAG =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "frog_glimmer_no_eat"));
    private static final int PASSIVE_REFRESH_DURATION = 20 * 4;
    private static final int ABILITY_COOLDOWN_TICKS = 20 * 120;
    private static final double EAT_RANGE = 8.0D;
    private static final float GROWTH_PER_EAT_MIN = 0.05F;
    private static final float GROWTH_PER_EAT_MAX = 0.6F;
    private static final float GROWTH_MAX_SCALE = 2.2F;
    private static final double GROWTH_DECAY_PER_TICK = 0.0006D;

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, path);
    }

    @Override
    public void registerGoals(GlimmerEntity entity) {
        entity.addGoal(0, new FloatGoal(entity));
        entity.addGoal(1, new MeleeAttackGoal(entity, 1.1D, true));
        entity.addGoal(5, new WaterAvoidingRandomStrollGoal(entity, 1.0D));
        entity.addGoal(6, new LookAtPlayerGoal(entity, Player.class, 8.0F));
        entity.addGoal(7, new RandomLookAroundGoal(entity));
        entity.addTargetGoal(2, new NearestAttackableTargetGoal<>(entity, MagmaCube.class, 10, true, false,
                magmaCube -> ((MagmaCube) magmaCube).getSize() == SMALL_SLIME_SIZE));
    }

    @Override
    public void onHurtTarget(GlimmerEntity entity, Entity target) {
        if (!(target instanceof MagmaCube magmaCube) || magmaCube.isAlive() || magmaCube.getSize() != SMALL_SLIME_SIZE) {
            return;
        }

        entity.spawnAtLocation(AntarchyObjects.LUMEN_FROGLIGHT.get());
    }

    @Override
    public ResourceLocation modelGeo() {
        return rl("geo/apple_cow.geo.json");
    }

    @Override
    public ResourceLocation animationFile() {
        return rl("animations/apple_cow.animation.json");
    }

    @Override
    public ResourceLocation texture(GlimmerEntity entity) {
        return rl("textures/entity/glimmer/frog_glimmer.png");
    }

    @Override
    public ResourceLocation emissiveTexture(GlimmerEntity entity) {
        return rl("textures/entity/glimmer/frog_glimmer_emmisive.png");
    }

    @Override
    public SoundEvent ambientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    public SoundEvent hurtSound() {
        return SoundEvents.FROG_HURT;
    }

    @Override
    public SoundEvent deathSound() {
        return SoundEvents.FROG_DEATH;
    }

    @Override
    public EntityDimensions adultDimensions() {
        // Matches vanilla Frog's real hitbox.
        return EntityDimensions.scalable(0.5F, 0.5F);
    }

    @Override
    public void tickPassive(GlimmerEntity entity, Player owner) {
        owner.addEffect(new MobEffectInstance(MobEffects.JUMP, PASSIVE_REFRESH_DURATION, 0, false, false, true));
    }

    @Override
    public int abilityCooldownTicks() {
        return ABILITY_COOLDOWN_TICKS;
    }

    @Override
    public double growthDecayPerTick() {
        return GROWTH_DECAY_PER_TICK;
    }

    @Override
    public void tickAbilityCheck(GlimmerEntity entity, Player owner) {
        LivingEntity target = findEdibleTarget(entity, owner);
        if (target != null) {
            this.eatTarget(entity, owner, target);
        }
    }

    private static LivingEntity findEdibleTarget(GlimmerEntity entity, Player owner) {
        AABB area = owner.getBoundingBox().inflate(EAT_RANGE);
        LivingEntity best = null;
        double bestDistSq = EAT_RANGE * EAT_RANGE;
        for (Entity candidate : owner.level().getEntities(entity, area)) {
            if (!(candidate instanceof Enemy) || !(candidate instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            if (isExcluded(living)) {
                continue;
            }
            double distSq = candidate.distanceToSqr(owner);
            if (distSq > bestDistSq) {
                continue;
            }
            bestDistSq = distSq;
            best = living;
        }
        return best;
    }

    private static boolean isExcluded(LivingEntity target) {
        if (target instanceof MultipartEntityOwner || target instanceof EnderDragon || target instanceof WitherBoss) {
            return true;
        }
        return target.getType().is(NO_EAT_TAG);
    }

    private void eatTarget(GlimmerEntity entity, Player owner, LivingEntity target) {
        float growthAdd = Mth.clamp(target.getMaxHealth() / 40.0F, GROWTH_PER_EAT_MIN, GROWTH_PER_EAT_MAX);
        entity.setGrowthScale(Math.min(GROWTH_MAX_SCALE, entity.getGrowthScale() + growthAdd));

        Component targetName = target.getDisplayName();
        entity.playSound(SoundEvents.FROG_EAT, 1.2F, 0.9F);
        target.teleportTo(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ());
        target.remove(Entity.RemovalReason.DISCARDED);

        this.sendAbilityMessage(owner, Component.translatable("entity.antarchy.glimmer.ability.frog", targetName));
        entity.startAbilityCooldown();
    }
}
