package com.craisinlord.antarchy.content.entity.glimmer;

import com.craisinlord.antarchy.Antarchy;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

public class GlimmerOuranwoodDeerBehavior implements GlimmerVariantBehavior {
    private static final int PASSIVE_REFRESH_DURATION = 20 * 4;
    private static final int ABILITY_COOLDOWN_TICKS = 20 * 10;
    private static final double FOLLOW_SPEED_MODIFIER = 1.75D;
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    private static final double CHARGE_RANGE = 12.0D;
    private static final double CHARGE_CONE_DOT = 0.5D;
    private static final double CHARGE_CORRIDOR_WIDTH = 1.75D;
    private static final float CHARGE_DAMAGE = 9.0F;
    private static final double CHARGE_KNOCKBACK = 1.6D;
    private static final int CHARGE_ANIM_TICKS = 45;

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, path);
    }

    @Override
    public void registerGoals(GlimmerEntity entity) {
        entity.addGoal(0, new FloatGoal(entity));
        entity.addGoal(5, new WaterAvoidingRandomStrollGoal(entity, 1.15D));
        entity.addGoal(6, new LookAtPlayerGoal(entity, Player.class, 8.0F));
        entity.addGoal(7, new RandomLookAroundGoal(entity));
    }

    @Override
    public ResourceLocation modelGeo() {
        return rl("geo/ouranwood_deer.geo.json");
    }

    @Override
    public ResourceLocation animationFile() {
        return rl("animations/ouranwood_deer.animation.json");
    }

    @Override
    public ResourceLocation texture(GlimmerEntity entity) {
        return rl("textures/entity/glimmer/deer_glimmer.png");
    }

    @Override
    public ResourceLocation emissiveTexture(GlimmerEntity entity) {
        return rl("textures/entity/glimmer/deer_glimmer_emissive.png");
    }

    @Override
    public SoundEvent ambientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    public SoundEvent hurtSound() {
        return SoundEvents.FOX_HURT;
    }

    @Override
    public SoundEvent deathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public EntityDimensions adultDimensions() {
        return EntityDimensions.scalable(1.35F, 2.1F);
    }

    @Override
    public EntityDimensions babyDimensions() {
        return EntityDimensions.scalable(0.9F, 1.4F);
    }

    @Override
    public void tickPassive(GlimmerEntity entity, Player owner) {
        owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, PASSIVE_REFRESH_DURATION, 0, false, false, true));
    }

    @Override
    public double followSpeedModifier() {
        return FOLLOW_SPEED_MODIFIER;
    }

    @Override
    @Nullable
    public RawAnimation movingAnimation(GlimmerEntity entity) {
        return entity.isFollowingOwner() ? RUN_ANIM : null;
    }

    @Override
    public int abilityCooldownTicks() {
        return ABILITY_COOLDOWN_TICKS;
    }

    @Override
    @Nullable
    public RawAnimation abilityAnimation() {
        return RawAnimation.begin().thenPlay("charge_start").thenPlay("charge_loop").thenPlay("charge_end");
    }

    @Override
    public void tickAbilityCheck(GlimmerEntity entity, Player owner) {
        if (!owner.isSprinting()) {
            return;
        }

        Vec3 look = owner.getLookAngle();
        LivingEntity bestTarget = null;
        double bestDistSq = CHARGE_RANGE * CHARGE_RANGE;
        for (Entity candidate : owner.level().getEntities(owner, owner.getBoundingBox().inflate(CHARGE_RANGE))) {
            if (!(candidate instanceof Enemy) || !(candidate instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            Vec3 toTarget = candidate.position().subtract(owner.position());
            double distSq = toTarget.lengthSqr();
            if (distSq > bestDistSq) {
                continue;
            }
            if (toTarget.normalize().dot(look) < CHARGE_CONE_DOT) {
                continue;
            }
            bestDistSq = distSq;
            bestTarget = living;
        }

        if (bestTarget != null) {
            this.performCharge(entity, owner, bestTarget);
        }
    }

    private void performCharge(GlimmerEntity entity, Player owner, LivingEntity target) {
        Vec3 from = entity.position();
        Vec3 direction = target.position().subtract(from);
        double horizontalDistance = Math.max(1.0D, direction.horizontalDistance());
        double dirX = direction.x / horizontalDistance;
        double dirZ = direction.z / horizontalDistance;

        entity.setDeltaMovement(dirX * 1.4D, entity.getDeltaMovement().y, dirZ * 1.4D);
        entity.playAbilityAnimation(CHARGE_ANIM_TICKS);
        entity.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);

        AABB corridor = new AABB(from, target.position()).inflate(CHARGE_CORRIDOR_WIDTH, 1.5D, CHARGE_CORRIDOR_WIDTH);
        for (Entity candidate : owner.level().getEntities(entity, corridor)) {
            if (!(candidate instanceof Enemy) || !(candidate instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            living.hurt(owner.damageSources().mobAttack(entity), CHARGE_DAMAGE);
            Vec3 away = living.position().subtract(from);
            double awayDistance = Math.max(0.1D, away.horizontalDistance());
            living.knockback(CHARGE_KNOCKBACK, -away.x / awayDistance, -away.z / awayDistance);
        }

        this.sendAbilityMessage(owner, Component.translatable("entity.antarchy.glimmer.ability.deer"));
        entity.startAbilityCooldown();
    }
}
