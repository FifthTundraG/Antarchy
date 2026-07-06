package com.craisinlord.antarchy.content.entity;

import com.craisinlord.antarchy.config.AntarchySettings;
import com.craisinlord.antarchy.content.AntarchyTags;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
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

public class RollyPollyEntity extends TamableAnimal implements GeoEntity {
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ROLL_UP_ANIM = RawAnimation.begin().thenPlay("wheel_mode");
    private static final RawAnimation ROLLING_ANIM = RawAnimation.begin().thenLoop("roll");
    private static final RawAnimation UNROLL_ANIM = RawAnimation.begin().thenPlay("normal_mode");

    private static final int ANIM_IDLE = 0;
    private static final int ANIM_WALK = 1;
    private static final int ANIM_ROLL_UP = 2;
    private static final int ANIM_ROLLING = 3;
    private static final int ANIM_UNROLL = 4;

    // wheel_mode is 0.75s, normal_mode is ~0.42s
    private static final int ROLL_UP_TICKS = 15;
    private static final int UNROLL_TICKS = 9;
    private static final int DEFENSIVE_CURL_TICKS = 100;
    private static final int TUMBLE_DAMAGE_INTERVAL_TICKS = 30;

    private static final EntityDataAccessor<Integer> ANIMATION_STATE =
            SynchedEntityData.defineId(RollyPollyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SADDLED =
            SynchedEntityData.defineId(RollyPollyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROLLED =
            SynchedEntityData.defineId(RollyPollyEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int rollTransitionTicks;
    private boolean unrolling;
    private int defensiveCurlTicks;
    private int tumbleTicks;

    public RollyPollyEntity(EntityType<? extends RollyPollyEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, AntarchySettings.rollyPollyHealth())
                .add(Attributes.MOVEMENT_SPEED, AntarchySettings.rollyPollyMovementSpeed())
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANIMATION_STATE, ANIM_IDLE);
        builder.define(SADDLED, false);
        builder.define(ROLLED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D) {
            @Override
            public boolean canUse() {
                // Curled-up or tamed rolly pollies stand their ground
                return !RollyPollyEntity.this.isRolled() && !RollyPollyEntity.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.1D, Ingredient.of(AntarchyTags.Items.ROLLY_POLLY_FOOD), false) {
            @Override
            public boolean canUse() {
                return !RollyPollyEntity.this.isRolled() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D) {
            @Override
            public boolean canUse() {
                return !RollyPollyEntity.this.isRolled() && !RollyPollyEntity.this.isVehicle() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(AntarchyTags.Items.ROLLY_POLLY_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (this.isFood(stack)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                if (!this.isTame()) {
                    if (this.random.nextInt(AntarchySettings.rollyPollyTameChance()) == 0) {
                        this.setTame(true, true);
                        this.setOwnerUUID(player.getUUID());
                        this.stopDefensiveCurl();
                        this.setTarget(null);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                } else if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(4.0F);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.isTame()) {
            if (!this.hasSaddle() && stack.is(Items.SADDLE) && this.isOwnedBy(player)) {
                if (!this.level().isClientSide) {
                    this.setSaddled(true);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    this.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 1.2F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (this.hasSaddle() && stack.isEmpty() && player.isShiftKeyDown() && this.isOwnedBy(player) && !player.isPassengerOfSameVehicle(this)) {
                if (!this.level().isClientSide) {
                    this.setSaddled(false);
                    this.spawnAtLocation(new ItemStack(Items.SADDLE));
                    this.playSound(SoundEvents.HORSE_SADDLE, 1.0F, 0.9F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (this.hasSaddle() && stack.isEmpty() && !player.isPassengerOfSameVehicle(this)) {
                if (!this.level().isClientSide) {
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    public boolean hasSaddle() {
        return this.entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, saddled);
    }

    public boolean isRolled() {
        return this.entityData.get(ROLLED);
    }

    private void setRolled(boolean rolled) {
        this.entityData.set(ROLLED, rolled);
    }

    /** Toggles wheel mode; called from the ridden-roll keybind payload. */
    public void handleRollToggle(ServerPlayer player) {
        if (!this.isTame() || !this.hasSaddle() || this.getControllingPassenger() != player) {
            return;
        }
        if (this.rollTransitionTicks > 0) {
            return;
        }
        if (this.isRolled()) {
            this.startUnroll();
        } else {
            this.startRollUp();
        }
    }

    private void startRollUp() {
        this.setRolled(true);
        this.unrolling = false;
        this.rollTransitionTicks = ROLL_UP_TICKS;
        this.setAnimationState(ANIM_ROLL_UP);
        this.playSound(SoundEvents.ARMOR_EQUIP_TURTLE.value(), 0.8F, 1.4F);
    }

    private void startUnroll() {
        this.unrolling = true;
        this.rollTransitionTicks = UNROLL_TICKS;
        this.setAnimationState(ANIM_UNROLL);
        this.playSound(SoundEvents.ARMOR_EQUIP_TURTLE.value(), 0.8F, 1.1F);
    }

    private void stopDefensiveCurl() {
        this.defensiveCurlTicks = 0;
        if (this.isRolled() && !this.isVehicle()) {
            this.startUnroll();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isRolled()) {
            amount *= 0.25F;
        }
        boolean hurt = super.hurt(source, amount);
        // Wild rolly pollies curl up defensively when attacked
        if (hurt && !this.level().isClientSide && !this.isTame() && this.isAlive()
                && !this.isRolled() && this.rollTransitionTicks <= 0) {
            this.defensiveCurlTicks = DEFENSIVE_CURL_TICKS;
            this.startRollUp();
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        if (this.rollTransitionTicks > 0) {
            this.rollTransitionTicks--;
            if (this.rollTransitionTicks <= 0) {
                if (this.unrolling) {
                    this.setRolled(false);
                    this.unrolling = false;
                    this.setAnimationState(ANIM_IDLE);
                } else {
                    this.setAnimationState(ANIM_ROLLING);
                }
            }
        }

        if (this.defensiveCurlTicks > 0) {
            this.defensiveCurlTicks--;
            this.getNavigation().stop();
            if (this.defensiveCurlTicks <= 0 && this.isRolled() && !this.isVehicle()) {
                this.startUnroll();
            }
        }

        this.tickTumbleDamage();
        this.updateAnimationState();
    }

    private void tickTumbleDamage() {
        if (!this.isRolled() || this.rollTransitionTicks > 0
                || !(this.getControllingPassenger() instanceof Player rider)) {
            this.tumbleTicks = 0;
            return;
        }
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 0.003D;
        boolean helmeted = !rider.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        if (!moving || helmeted) {
            return;
        }
        if (++this.tumbleTicks >= TUMBLE_DAMAGE_INTERVAL_TICKS) {
            this.tumbleTicks = 0;
            rider.hurt(this.damageSources().generic(), (float) AntarchySettings.rollyPollyTumbleDamage());
        }
    }

    private void updateAnimationState() {
        // Roll states are driven by the transition/tick logic above
        int state = this.getAnimationState();
        if (state == ANIM_ROLL_UP || state == ANIM_ROLLING || state == ANIM_UNROLL) {
            return;
        }
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;
        this.setAnimationState(moving ? ANIM_WALK : ANIM_IDLE);
    }

    private int getAnimationState() {
        return this.entityData.get(ANIMATION_STATE);
    }

    private void setAnimationState(int state) {
        this.entityData.set(ANIMATION_STATE, state);
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        if (!this.isTame() || !this.hasSaddle()) {
            return null;
        }
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            // When rolled the rider tucks into the center of the ball; otherwise sits on the shell
            double yOffset = this.isRolled() ? -0.25D : 0.45D;
            moveFunction.accept(passenger, this.getX(), this.getY() + yOffset, this.getZ());
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (!(this.getControllingPassenger() instanceof Player rider) || !this.isVehicle()) {
            if (this.defensiveCurlTicks > 0) {
                super.travel(Vec3.ZERO);
                return;
            }
            super.travel(travelVector);
            return;
        }

        if (this.rollTransitionTicks > 0) {
            // Hold still while rolling up / unrolling
            super.travel(Vec3.ZERO);
            return;
        }

        this.setYRot(rider.getYHeadRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        float forward = rider.zza;
        float strafe = rider.xxa * 0.5F;
        if (forward < 0.0F) {
            forward *= 0.4F;
        }
        if (this.isRolled()) {
            // Rolling is fast but drifty
            strafe *= 0.35F;
        }

        double speed = this.getAttributeValue(Attributes.MOVEMENT_SPEED)
                * (this.isRolled() ? AntarchySettings.rollyPollyRollSpeedMultiplier() : 1.0D);
        this.setSpeed((float) speed);

        Vec3 input = new Vec3(strafe, 0.0D, forward);
        Vec3 motion = new Vec3(0.0D, this.getDeltaMovement().y, 0.0D);
        this.moveRelative(this.getSpeed(), input);
        this.setDeltaMovement(motion.add(this.getDeltaMovement().x, 0.0D, this.getDeltaMovement().z));
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource damageSource) {
        if (this.isRolled()) {
            return false;
        }
        return super.causeFallDamage(fallDistance, multiplier, damageSource);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.hasSaddle()) {
            this.spawnAtLocation(new ItemStack(Items.SADDLE));
            this.setSaddled(false);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Saddled", this.hasSaddle());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setSaddled(tag.getBoolean("Saddled"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 2, this::mainAnimController));
    }

    private PlayState mainAnimController(AnimationState<RollyPollyEntity> state) {
        return switch (this.getAnimationState()) {
            case ANIM_WALK -> state.setAndContinue(WALK_ANIM);
            case ANIM_ROLL_UP -> state.setAndContinue(ROLL_UP_ANIM);
            case ANIM_ROLLING -> state.setAndContinue(ROLLING_ANIM);
            case ANIM_UNROLL -> state.setAndContinue(UNROLL_ANIM);
            default -> state.setAndContinue(IDLE_ANIM);
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
