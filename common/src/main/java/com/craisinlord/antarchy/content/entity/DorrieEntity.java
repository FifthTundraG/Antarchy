package com.craisinlord.antarchy.content.entity;

import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.config.AntarchySettings;
import com.craisinlord.antarchy.content.item.CheepOnAStickItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.registries.BuiltInRegistries;
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
import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
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
    private static final float BASE_SPEED = 0.6F;
    private static final float BOOSTED_SPEED = 1.3F;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation JUMP_START_ANIM = RawAnimation.begin().thenPlay("jump_1");
    private static final RawAnimation JUMP_LOOP_ANIM = RawAnimation.begin().thenLoop("jump_2");
    private static final RawAnimation JUMP_LAND_ANIM = RawAnimation.begin().thenPlay("jump_3");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int chargeTicks = 0;
    private boolean isCharging = false;
    private boolean isLeaping = false;
    private boolean wasInAir = false;
    private int landAnimTicks = 0;
    private boolean isPressingJump = false;

    public DorrieEntity(EntityType<? extends DorrieEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, BASE_SPEED, 1.0F, true);
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
                .add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setTamed(tag.getBoolean("Tamed"));
        this.setSaddle(tag.getBoolean("HasSaddle"));
    }


    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!this.isTamed()) {
            if (!this.level().isClientSide && this.isCheepItem(stack)) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
                if (this.random.nextInt(3) == 0) {
                    this.setTamed(true);
                    this.playSound(SoundEvents.HORSE_BREATHE, 0.5F, 1.0F);
                } else {
                    this.playSound(SoundEvents.DOLPHIN_HURT, 0.5F, 1.0F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
            return super.mobInteract(player, hand);
        }

        // Remove saddle with empty hand
        if (this.hasSaddle() && stack.isEmpty() && !player.isPassengerOfSameVehicle(this)) {
            if (!this.level().isClientSide) {
                this.setSaddle(false);
                this.spawnAtLocation(new ItemStack(Items.SADDLE));
                this.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Apply saddle
        if (!this.hasSaddle() && stack.is(Items.SADDLE)) {
            if (!this.level().isClientSide) {
                this.setSaddle(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                this.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        // Mount
        if (this.hasSaddle() && !player.isPassengerOfSameVehicle(this)) {
            if (!this.level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return this.isCheepItem(stack);
    }


    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        if (!this.isTamed() || !this.hasSaddle()) return null;
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player) return player;
        return null;
    }

    private boolean riderHasCheepStick(Player player) {
        return player.getMainHandItem().getItem() instanceof CheepOnAStickItem
                || player.getOffhandItem().getItem() instanceof CheepOnAStickItem;
    }

    private ItemStack getCheepStick(Player player) {
        if (player.getMainHandItem().getItem() instanceof CheepOnAStickItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof CheepOnAStickItem) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        if (!riderHasCheepStick(player)) return Vec3.ZERO;
        float forward = player.zza > 0 ? player.zza : player.zza / 4.0F;
        float strafe = player.xxa * 0.5F;
        return new Vec3(strafe, travelVector.y, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        float speed = BASE_SPEED;
        if (riderHasCheepStick(player)) {
            speed = BOOSTED_SPEED;
            if (!this.level().isClientSide) {
                ItemStack stick = getCheepStick(player);
                if (!stick.isEmpty() && this.tickCount % 10 == 0) {
                    stick.hurtAndBreak(1, player,
                            player.getMainHandItem().getItem() instanceof CheepOnAStickItem
                                    ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                                    : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
                }
            }
        }
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * speed;
    }


    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            LivingEntity passenger = this.getControllingPassenger();

            if (passenger != null && isPressingJump) {
                Vec3 mov = this.getDeltaMovement();
                double boost = this.isInWater() ? 0.1D : 0.04D;
                this.setDeltaMovement(mov.x, Math.min(mov.y + boost, 0.5D), mov.z);
                this.hasImpulse = true;
            }

            if (passenger instanceof Player && isCharging) {
                chargeTicks = Math.min(chargeTicks + 1, MAX_CHARGE_TICKS);
                setJumpCharge((int) ((chargeTicks / (float) MAX_CHARGE_TICKS) * 100));
            }
        }

        if (landAnimTicks > 0) landAnimTicks--;

        boolean inAirNow = !this.onGround() && !this.isInWater() && !this.isInLava();
        if (wasInAir && !inAirNow && isLeaping) {
            isLeaping = false;
            landAnimTicks = 12;
        }
        wasInAir = inAirNow;
    }


    /** Called when the rider presses/releases space (up movement). */
    public void setPressingJump(boolean pressing) {
        this.isPressingJump = pressing;
    }

    /** Called when the rider presses left-control (charge start). */
    public void startJumpCharge() {
        if (!this.level().isClientSide) {
            isCharging = true;
        }
    }

    /** Called when the rider releases left-control (charge release). */
    public void releaseJump() {
        if (!this.level().isClientSide && isCharging) {
            float power = chargeTicks / (float) MAX_CHARGE_TICKS;
            if (power > 0.05F) {
                double horizontal = 0.8D + power * 1.4D;
                double vertical = 0.4D + power * 0.8D;
                Vec3 look = this.getLookAngle();
                Vec3 current = this.getDeltaMovement();
                this.setDeltaMovement(
                        look.x * horizontal * 0.7D + current.x * 0.3D,
                        vertical,
                        look.z * horizontal * 0.7D + current.z * 0.3D
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
            moveFunction.accept(passenger,
                    this.getX(),
                    this.getY() + this.getPassengersRidingOffset() + passenger.getBbHeight() * 0.5D,
                    this.getZ());
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
        if (landAnimTicks > 0) return state.setAndContinue(JUMP_LAND_ANIM);
        if (isLeaping) {
            return state.setAndContinue(wasInAir ? JUMP_LOOP_ANIM : JUMP_START_ANIM);
        }
        return state.setAndContinue(state.isMoving() ? SWIM_ANIM : IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private boolean isCheepItem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BuiltInRegistries.ITEM.getOptional(CHEEP_ITEM_ID).orElse(Items.AIR));
    }

    private static class HuntCheepGoal extends Goal {
        private static final int COOLDOWN_TICKS = 600; // 30 seconds
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
            if (dorrie.level().isClientSide) return false;
            if (cooldown > 0) { cooldown--; return false; }
            List<CheepEntity> nearby = dorrie.level().getEntitiesOfClass(
                    CheepEntity.class,
                    dorrie.getBoundingBox().inflate(HUNT_RANGE)
            );
            if (nearby.isEmpty()) return false;
            nearby.sort((a, b) -> Double.compare(a.distanceToSqr(dorrie), b.distanceToSqr(dorrie)));
            target = nearby.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && dorrie.distanceToSqr(target) <= HUNT_RANGE * HUNT_RANGE * 4;
        }

        @Override
        public void start() {
            dorrie.getNavigation().moveTo(target, 1.4D);
        }

        @Override
        public void tick() {
            if (target == null) return;
            dorrie.getLookControl().setLookAt(target, 30.0F, 30.0F);
            dorrie.getNavigation().moveTo(target, 1.4D);
            if (dorrie.distanceToSqr(target) <= ATTACK_RANGE_SQ) {
                target.hurt(dorrie.damageSources().mobAttack(dorrie), (float) dorrie.getAttributeValue(Attributes.ATTACK_DAMAGE));
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
