package com.craisinlord.antarchy.content.entity;

import com.craisinlord.antarchy.config.AntarchySettings;
import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.content.AntarchyTags;
import com.craisinlord.antarchy.content.GlimmerParticles;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GlimmerEntity extends Animal implements GeoEntity {
    private static final EntityDataAccessor<Integer> GLOW_TICKS =
            SynchedEntityData.defineId(GlimmerEntity.class, EntityDataSerializers.INT);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    private static final String QUIRK_CONTROLLER = "quirk_controller";
    private static final String QUIRK_TRIGGER = "quirk";

    private static final int TRUST_REQUIRED_FEEDS = 3;
    private static final int GRACE_DURATION_TICKS = 2400;
    private static final int GRACE_COOLDOWN_TICKS = 6000;
    private static final int GLOW_LIGHT_THRESHOLD = 7;
    private static final int GLOW_CHECK_INTERVAL_TICKS = 20;
    private static final int GLOW_TRANSITION_TICKS = 10;
    private static final double WALK_SPEED_THRESHOLD_SQR = 0.0009D;
    private static final double RUN_SPEED_THRESHOLD_SQR = 0.0035D;

    private static final int MIN_QUIRK_COOLDOWN = 20 * 8;
    private static final int MAX_QUIRK_COOLDOWN = 20 * 25;

    private static final String TRUST_KEY = "GlimmerTrust";
    private static final String TRUST_PLAYER_KEY = "Player";
    private static final String TRUST_FEED_COUNT_KEY = "FeedCount";
    private static final String TRUST_TRUSTED_KEY = "Trusted";
    private static final String TRUST_COOLDOWN_KEY = "GraceCooldown";
    private static final String GLOWING_SAVE_KEY = "Glowing";

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, TrustEntry> trustByPlayer = new HashMap<>();

    private boolean glowing;
    private int quirkCooldown = createQuirkCooldown();

    public GlimmerEntity(EntityType<? extends GlimmerEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, AntarchySettings.glimmerHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GLOW_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.1D, Ingredient.of(AntarchyTags.Items.GLIMMER_FOOD), false));
        this.goalSelector.addGoal(3, new AvoidUntrustedPlayerGoal(10.0F, 1.15D));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, Monster.class, 8.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(AntarchyTags.Items.GLIMMER_FOOD);
    }

    @Override
    public GlimmerEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(AntarchyTags.Items.GLIMMER_FOOD)) {
            return this.feedGlimmerFood(player, stack, hand);
        }
        return super.mobInteract(player, hand);
    }

    private InteractionResult feedGlimmerFood(Player player, ItemStack stack, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.CONSUME;
        }

        this.usePlayerItem(player, hand, stack);
        this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F + (this.random.nextFloat() - 0.5F) * 0.2F);

        if (!this.isBaby()) {
            TrustEntry entry = this.trustByPlayer.computeIfAbsent(player.getUUID(), id -> new TrustEntry());
            if (!entry.trusted) {
                entry.feedCount++;
                if (entry.feedCount >= TRUST_REQUIRED_FEEDS) {
                    entry.trusted = true;
                }
            } else if (this.glowing && entry.graceCooldown <= 0) {
                player.addEffect(new MobEffectInstance(AntarchyObjects.GLIMMERS_GRACE.get(), GRACE_DURATION_TICKS, 0, false, true, true));
                entry.graceCooldown = GRACE_COOLDOWN_TICKS;
            }
        }

        return InteractionResult.SUCCESS;
    }

    public boolean isTrusted(UUID playerId) {
        TrustEntry entry = this.trustByPlayer.get(playerId);
        return entry != null && entry.trusted;
    }

    private boolean hasAnyTrustProgress() {
        for (TrustEntry entry : this.trustByPlayer.values()) {
            if (entry.trusted || entry.feedCount > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return super.isPersistenceRequired() || (!this.isBaby() && this.hasAnyTrustProgress());
    }

    public boolean isGlowingState() {
        return this.getGlowTicks() > 0;
    }

    public float getGlowBlend(float partialTick) {
        return Mth.clamp((this.getGlowTicks() + partialTick) / (float) GLOW_TRANSITION_TICKS, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.quirkCooldown > 0) {
            this.quirkCooldown--;
        }

        if (!this.level().isClientSide && this.tickCount % GLOW_CHECK_INTERVAL_TICKS == 0) {
            this.glowing = this.level().getMaxLocalRawBrightness(this.blockPosition()) <= GLOW_LIGHT_THRESHOLD;
        }

        if (!this.level().isClientSide) {
            int glowTicks = this.getGlowTicks();
            if (this.glowing && glowTicks < GLOW_TRANSITION_TICKS) {
                this.setGlowTicks(glowTicks + 1);
            } else if (!this.glowing && glowTicks > 0) {
                this.setGlowTicks(glowTicks - 1);
            }
        }

        if (this.isGlowingState()) {
            GlimmerParticles.tickAmbient(this, this.getDeltaMovement().horizontalDistanceSqr() > RUN_SPEED_THRESHOLD_SQR);
        }

        if (this.level().isClientSide) {
            return;
        }

        for (TrustEntry entry : this.trustByPlayer.values()) {
            if (entry.graceCooldown > 0) {
                entry.graceCooldown--;
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(GLOWING_SAVE_KEY, this.glowing);

        ListTag trustList = new ListTag();
        for (Map.Entry<UUID, TrustEntry> entry : this.trustByPlayer.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(TRUST_PLAYER_KEY, entry.getKey());
            entryTag.putInt(TRUST_FEED_COUNT_KEY, entry.getValue().feedCount);
            entryTag.putBoolean(TRUST_TRUSTED_KEY, entry.getValue().trusted);
            entryTag.putInt(TRUST_COOLDOWN_KEY, entry.getValue().graceCooldown);
            trustList.add(entryTag);
        }
        tag.put(TRUST_KEY, trustList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.glowing = tag.getBoolean(GLOWING_SAVE_KEY);
        this.setGlowTicks(this.glowing ? GLOW_TRANSITION_TICKS : 0);

        this.trustByPlayer.clear();
        for (net.minecraft.nbt.Tag rawEntry : tag.getList(TRUST_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag entryTag = (CompoundTag) rawEntry;
            TrustEntry entry = new TrustEntry();
            entry.feedCount = entryTag.getInt(TRUST_FEED_COUNT_KEY);
            entry.trusted = entryTag.getBoolean(TRUST_TRUSTED_KEY);
            entry.graceCooldown = entryTag.getInt(TRUST_COOLDOWN_KEY);
            this.trustByPlayer.put(entryTag.getUUID(TRUST_PLAYER_KEY), entry);
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData spawnGroupData) {
        GlimmerSpawnData glimmerSpawnData = spawnGroupData instanceof GlimmerSpawnData data ? data : new GlimmerSpawnData();
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);

        if ((spawnReason == MobSpawnType.NATURAL || spawnReason == MobSpawnType.CHUNK_GENERATION)) {
            if (glimmerSpawnData.adultSpawned) {
                this.setAge(-24000);
            } else {
                glimmerSpawnData.adultSpawned = true;
            }
        }

        return result != null ? result : glimmerSpawnData;
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions dimensions = super.getDefaultDimensions(pose);
        if (this.isBaby()) {
            return dimensions;
        }
        return net.minecraft.world.entity.EntityDimensions.scalable(dimensions.width() * 2.0F, dimensions.height() * 2.0F);
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        this.refreshDimensions();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 0, this::mainAnimController));
        controllers.add(new AnimationController<>(this, QUIRK_CONTROLLER, 0, state -> PlayState.STOP)
                .triggerableAnim(QUIRK_TRIGGER, RawAnimation.begin().thenPlay("quirk")));
    }

    private PlayState mainAnimController(AnimationState<GlimmerEntity> state) {
        double speedSqr = this.getDeltaMovement().horizontalDistanceSqr();
        if (speedSqr > RUN_SPEED_THRESHOLD_SQR) {
            return state.setAndContinue(RUN_ANIM);
        }
        if (speedSqr > WALK_SPEED_THRESHOLD_SQR) {
            return state.setAndContinue(WALK_ANIM);
        }

        if (this.quirkCooldown <= 0) {
            this.quirkCooldown = this.createQuirkCooldown();
            this.triggerAnim(QUIRK_CONTROLLER, QUIRK_TRIGGER);
        }

        return state.setAndContinue(IDLE_ANIM);
    }

    private int createQuirkCooldown() {
        return MIN_QUIRK_COOLDOWN + this.random.nextInt(MAX_QUIRK_COOLDOWN - MIN_QUIRK_COOLDOWN + 1);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private int getGlowTicks() {
        return this.entityData.get(GLOW_TICKS);
    }

    private void setGlowTicks(int glowTicks) {
        this.entityData.set(GLOW_TICKS, Mth.clamp(glowTicks, 0, GLOW_TRANSITION_TICKS));
    }

    private static final class TrustEntry {
        int feedCount;
        boolean trusted;
        int graceCooldown;
    }

    private static final class GlimmerSpawnData implements SpawnGroupData {
        private boolean adultSpawned;
    }

    private final class AvoidUntrustedPlayerGoal extends Goal {
        private final float maxDistance;
        private final double speedModifier;
        private Player avoidTarget;
        private Path path;

        AvoidUntrustedPlayerGoal(float maxDistance, double speedModifier) {
            this.maxDistance = maxDistance;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (GlimmerEntity.this.isBaby()) {
                return false;
            }

            Player nearest = GlimmerEntity.this.level().getNearestPlayer(GlimmerEntity.this, this.maxDistance);
            if (nearest == null || nearest.isSpectator() || nearest.isCreative()
                    || GlimmerEntity.this.isTrusted(nearest.getUUID())
                    || GlimmerEntity.this.distanceToSqr(nearest) > (double) (this.maxDistance * this.maxDistance)) {
                return false;
            }

            Path escapePath = this.findEscapePath(nearest);
            if (escapePath == null) {
                return false;
            }

            this.avoidTarget = nearest;
            this.path = escapePath;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.avoidTarget != null
                    && this.avoidTarget.isAlive()
                    && !GlimmerEntity.this.isTrusted(this.avoidTarget.getUUID())
                    && !GlimmerEntity.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            GlimmerEntity.this.getNavigation().moveTo(this.path, this.speedModifier);
        }

        @Override
        public void stop() {
            this.avoidTarget = null;
            this.path = null;
        }

        @Nullable
        private Path findEscapePath(Player toAvoid) {
            Vec3 escapeVec = DefaultRandomPos.getPosAway(GlimmerEntity.this, 16, 7, toAvoid.position());
            if (escapeVec == null) {
                return null;
            }
            return GlimmerEntity.this.getNavigation().createPath(BlockPos.containing(escapeVec), 0);
        }
    }
}
