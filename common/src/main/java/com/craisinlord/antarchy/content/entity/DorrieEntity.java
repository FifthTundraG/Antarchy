package com.craisinlord.antarchy.content.entity;

import com.craisinlord.antarchy.config.AntarchySettings;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TryFindWaterGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DorrieEntity extends Animal implements GeoEntity {
    private static final ResourceLocation CHEEP_ITEM_ID = ResourceLocation.fromNamespaceAndPath("antarchy", "cheep");

    private static final EntityDataAccessor<Boolean> HAS_SADDLE =
            SynchedEntityData.defineId(DorrieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TAMED =
            SynchedEntityData.defineId(DorrieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> JUMP_CHARGE =
            SynchedEntityData.defineId(DorrieEntity.class, EntityDataSerializers.INT);

    private static final int MAX_CHARGE_TICKS = 40;
    private static final float WATER_RIDE_SPEED = 1.26F;
    private static final float LAND_BEACH_SPEED = 0.04F;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation JUMP_START_ANIM = RawAnimation.begin().thenPlay("jump_1");
    private static final RawAnimation JUMP_LOOP_ANIM = RawAnimation.begin().thenLoop("jump_2");
    private static final RawAnimation JUMP_LAND_ANIM = RawAnimation.begin().thenPlay("jump_3");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final int RIDES_TO_TAME = 4;
    private static final int BUCK_DELAY_TICKS = 60;

    private int chargeTicks = 0;
    private boolean isCharging = false;
    private boolean isLeaping = false;
    private boolean wasInAir = false;
    private int landAnimTicks = 0;
    private int rideAttempts = 0;
    private int buckTimer = 0;
    private int swimForwardTicks = 0;

    public DorrieEntity(EntityType<? extends DorrieEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 1.0F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAS_SADDLE, false);
        builder.define(TAMED, false);
        builder.define(JUMP_CHARGE, 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BreathAirGoal(this));
        this.goalSelector.addGoal(2, new TryFindWaterGoal(this));
        this.goalSelector.addGoal(3, new HuntCheepGoal(this));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, AntarchySettings.dorrieHealth())
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    public boolean hasSaddle() {
        return this.entityData.get(HAS_SADDLE);
    }

    public void setSaddle(boolean saddle) {
        this.entityData.set(HAS_SADDLE, saddle);
    }

    public boolean isTamed() {
        return this.entityData.get(TAMED);
    }

    public void setTamed(boolean tamed) {
        this.entityData.set(TAMED, tamed);
    }

    public int getJumpCharge() {
        return this.entityData.get(JUMP_CHARGE);
    }

    private void setJumpCharge(int charge) {
        this.entityData.set(JUMP_CHARGE, charge);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Tamed", this.isTamed());
        tag.putBoolean("HasSaddle", this.hasSaddle());
        tag.putInt("RideAttempts", this.rideAttempts);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setTamed(tag.getBoolean("Tamed"));
        this.setSaddle(tag.getBoolean("HasSaddle"));
        this.rideAttempts = tag.getInt("RideAttempts");
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTamed()) {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.hasSaddle() && stack.isEmpty() && !player.isPassengerOfSameVehicle(this)) {
            if (!this.level().isClientSide) {
                this.setSaddle(false);
                this.spawnAtLocation(new ItemStack(Items.SADDLE));
                this.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (!this.hasSaddle() && stack.is(Items.SADDLE)) {
            if (!this.level().isClientSide) {
                this.setSaddle(true);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                this.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.hasSaddle() && !player.isPassengerOfSameVehicle(this)) {
            if (!this.level().isClientSide) {
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        if (!this.isTamed() || !this.hasSaddle()) {
            return null;
        }
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player) {
            return player;
        }
        return null;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float forward = player.zza;
        float strafe = player.xxa * 0.5F;
        return new Vec3(strafe, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        float speed = (this.isInWater() || this.isInLava())
                ? WATER_RIDE_SPEED * (1.0F + Math.min(this.swimForwardTicks, 60) * 0.025F)
                : LAND_BEACH_SPEED;
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * speed;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getControllingPassenger() instanceof Player player) {
            this.setYRot(player.getYRot());
            this.yRotO = this.getYRot();
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            if (this.isInWater() || this.isInLava()) {
                if (player.zza > 0.05F) {
                    this.swimForwardTicks = Math.min(this.swimForwardTicks + 1, 60);
                } else {
                    this.swimForwardTicks = Math.max(this.swimForwardTicks - 2, 0);
                }
            } else {
                this.swimForwardTicks = 0;
            }
        } else {
            this.swimForwardTicks = 0;
        }

        if (!this.level().isClientSide && !this.isTamed() && this.getFirstPassenger() instanceof Player rider) {
            buckTimer++;
            if (buckTimer >= BUCK_DELAY_TICKS) {
                rider.stopRiding();
                buckTimer = 0;
                rideAttempts++;
                if (rideAttempts >= RIDES_TO_TAME) {
                    this.setTamed(true);
                    this.playSound(SoundEvents.HORSE_BREATHE, 0.8F, 1.0F);
                } else {
                    this.playSound(SoundEvents.HORSE_ANGRY, 0.8F, 1.0F);
                }
            }
        } else if (!this.isTamed() && this.getFirstPassenger() == null) {
            buckTimer = 0;
        }

        if (!this.level().isClientSide && this.getControllingPassenger() instanceof Player && isCharging) {
            chargeTicks = Math.min(chargeTicks + 1, MAX_CHARGE_TICKS);
            setJumpCharge((int) ((chargeTicks / (float) MAX_CHARGE_TICKS) * 100));
        }

        if (this.isInWater() && !isLeaping) {
            Vec3 mov = this.getDeltaMovement();
            double push = this.isUnderWater() ? 0.07D : 0.015D;
            double cappedY = this.isUnderWater() ? 0.3D : 0.05D;
            this.setDeltaMovement(mov.x, Math.min(mov.y + push, cappedY), mov.z);
        }

        if (landAnimTicks > 0) {
            landAnimTicks--;
        }

        boolean inAirNow = !this.onGround() && !this.isInWater() && !this.isInLava();
        if (wasInAir && !inAirNow && isLeaping) {
            isLeaping = false;
            landAnimTicks = 12;
        }
        wasInAir = inAirNow;
    }

    public void setPressingJump(boolean pressing) {}

    public void startJumpCharge() {
        if (!this.level().isClientSide) {
            isCharging = true;
        }
    }

    public void releaseJump() {
        if (!this.level().isClientSide && isCharging) {
            float power = chargeTicks / (float) MAX_CHARGE_TICKS;
            if (power > 0.05F) {
                Vec3 look = this.getLookAngle();
                Vec3 current = this.getDeltaMovement();
                double forwardAdd = power * 0.5D;
                double vertical    = 0.55D + power * 0.9D;
                this.setDeltaMovement(
                        current.x + look.x * forwardAdd,
                        vertical,
                        current.z + look.z * forwardAdd
                );
                this.hasImpulse = true;
                isLeaping = true;
            }
            isCharging = false;
            chargeTicks = 0;
            setJumpCharge(0);
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            Vec3 forward = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
            moveFunction.accept(passenger,
                    this.getX() - forward.x * 0.25D,
                    this.getY() + 0.5D,
                    this.getZ() - forward.z * 0.25D);
        }
    }

    public double getPassengersRidingOffset() {
        return 0.6D;
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.hasSaddle()) {
            this.spawnAtLocation(new ItemStack(Items.SADDLE));
        }
    }

    @Override
    @Nullable
    public DorrieEntity getBreedOffspring(ServerLevel level, AgeableMob mob) {
        return null;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DOLPHIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DOLPHIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DOLPHIN_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.DOLPHIN_SWIM;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 2, this::mainAnimController));
    }

    private PlayState mainAnimController(AnimationState<DorrieEntity> state) {
        if (landAnimTicks > 0) {
            return state.setAndContinue(JUMP_LAND_ANIM);
        }
        if (isLeaping) {
            return state.setAndContinue(wasInAir ? JUMP_LOOP_ANIM : JUMP_START_ANIM);
        }
        return state.setAndContinue(state.isMoving() ? SWIM_ANIM : IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return this.isCheepItem(stack);
    }

    private boolean isCheepItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BuiltInRegistries.ITEM.getOptional(CHEEP_ITEM_ID).orElse(Items.AIR));
    }

    private static class HuntCheepGoal extends Goal {
        private static final int COOLDOWN_TICKS = 600;
        private static final double HUNT_RANGE = 16.0D;
        private static final double ATTACK_RANGE_SQ = 2.5D * 2.5D;

        private final DorrieEntity dorrie;
        private CheepEntity target;
        private int cooldown = 0;

        HuntCheepGoal(DorrieEntity dorrie) {
            this.dorrie = dorrie;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (dorrie.level().isClientSide) {
                return false;
            }
            if (cooldown > 0) {
                cooldown--;
                return false;
            }
            if (dorrie.getControllingPassenger() != null) {
                return false;
            }
            List<CheepEntity> nearby = dorrie.level().getEntitiesOfClass(
                    CheepEntity.class, dorrie.getBoundingBox().inflate(HUNT_RANGE));
            if (nearby.isEmpty()) {
                return false;
            }
            nearby.sort((a, b) -> Double.compare(a.distanceToSqr(dorrie), b.distanceToSqr(dorrie)));
            target = nearby.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive()
                    && dorrie.getControllingPassenger() == null
                    && dorrie.distanceToSqr(target) <= HUNT_RANGE * HUNT_RANGE * 4;
        }

        @Override
        public void start() {
            dorrie.getNavigation().moveTo(target, 1.4D);
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            dorrie.getLookControl().setLookAt(target, 30.0F, 30.0F);
            dorrie.getNavigation().moveTo(target, 1.4D);
            if (dorrie.distanceToSqr(target) <= ATTACK_RANGE_SQ) {
                target.hurt(dorrie.damageSources().mobAttack(dorrie),
                        (float) dorrie.getAttributeValue(Attributes.ATTACK_DAMAGE));
                stop();
            }
        }

        @Override
        public void stop() {
            target = null;
            dorrie.getNavigation().stop();
            cooldown = COOLDOWN_TICKS;
        }
    }
}
