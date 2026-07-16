package com.craisinlord.antarchy.content.entity.kraken;

import com.craisinlord.antarchy.config.AntarchySettings;
import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.content.item.KrakensGraspItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class KrakensGraspThrownTrident extends AbstractArrow implements ItemSupplier {
    private boolean dealtDamage;
    private boolean returning;

    public KrakensGraspThrownTrident(EntityType<? extends KrakensGraspThrownTrident> entityType, Level level) {
        super(entityType, level);
    }

    public KrakensGraspThrownTrident(EntityType<? extends KrakensGraspThrownTrident> entityType, LivingEntity owner, Level level, ItemStack stack) {
        super(entityType, owner, level, stack, null);
        this.setBaseDamage(AntarchySettings.krakensGraspThrownDamage());
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(AntarchyObjects.KRAKENS_GRASP.get());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(AntarchyObjects.KRAKENS_GRASP.get());
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity owner = this.getOwner();
        int loyalty = this.effectiveLoyaltyLevel();
        if (loyalty > 0 && (this.dealtDamage || this.isNoPhysics()) && owner instanceof LivingEntity livingOwner && livingOwner.isAlive()) {
            this.setNoPhysics(true);
            Vec3 toOwner = owner.getEyePosition().subtract(this.position());
            this.setPosRaw(this.getX(), this.getY() + toOwner.y * 0.015D * loyalty, this.getZ());
            if (this.level().isClientSide) {
                this.yOld = this.getY();
            }

            double speed = 0.05D * loyalty;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(toOwner.normalize().scale(speed)));
            if (!this.returning) {
                this.returning = true;
                this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
            }
        }

        super.tick();
    }

    private int effectiveLoyaltyLevel() {
        return AntarchySettings.krakensGraspInnateLoyalty() ? Mth.clamp(AntarchySettings.krakensGraspInnateLoyaltyLevel(), 1, 3) : 0;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity target) || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity owner = this.getOwner();
        DamageSource damageSource = this.damageSources().trident(this, owner != null ? owner : this);
        boolean hurt = target.hurt(damageSource, (float) AntarchySettings.krakensGraspThrownDamage());
        if (hurt) {
            KrakensGraspItem.strikeLightning(target, serverLevel);
        }

        this.dealtDamage = true;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            TentacleEntity.spawnAt(serverLevel, result.getLocation(), this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null);
        }
    }
}
