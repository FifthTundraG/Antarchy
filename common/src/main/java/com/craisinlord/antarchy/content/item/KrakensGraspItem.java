package com.craisinlord.antarchy.content.item;

import com.craisinlord.antarchy.config.AntarchySettings;
import com.craisinlord.antarchy.content.AntarchyObjects;
import com.craisinlord.antarchy.content.client.model.ResourceBackedGeoItemModel;
import com.craisinlord.antarchy.content.client.renderer.AnimatedHeldItemRenderer;
import com.craisinlord.antarchy.content.entity.kraken.KrakensGraspThrownTrident;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class KrakensGraspItem extends TridentItem implements GeoItem {
    private static final ResourceLocation MODEL_LOCATION = ResourceLocation.fromNamespaceAndPath("antarchy", "geo/krakens_grasp.geo.json");
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("antarchy", "textures/item/krakens_grasp/krakens_grasp.png");
    private static final ResourceLocation ANIMATION_LOCATION = ResourceLocation.fromNamespaceAndPath("antarchy", "animations/static_item.animation.json");
    private static final ResourceLocation ATTACK_DAMAGE_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("antarchy", "krakens_grasp_attack_damage");
    private static final ResourceLocation ATTACK_SPEED_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("antarchy", "krakens_grasp_attack_speed");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public KrakensGraspItem(Item.Properties properties) {
        super(properties);
        GeoItem.registerSyncedAnimatable(this);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, AntarchySettings.krakensGraspAttackDamage(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER_ID, AntarchySettings.krakensGraspAttackSpeed(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.NONE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide) {
            ensureInnateLoyalty(stack, level.registryAccess());
        }
    }

    private static void ensureInnateLoyalty(ItemStack stack, HolderLookup.Provider registries) {
        if (!AntarchySettings.krakensGraspInnateLoyalty()) {
            return;
        }

        var loyalty = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY);
        int desiredLevel = net.minecraft.util.Mth.clamp(AntarchySettings.krakensGraspInnateLoyaltyLevel(), 1, 3);
        if (EnchantmentHelper.getItemEnchantmentLevel(loyalty, stack) >= desiredLevel) {
            return;
        }

        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(stack.getEnchantments());
        enchantments.set(loyalty, desiredLevel);
        EnchantmentHelper.setEnchantments(stack, enchantments.toImmutable().withTooltip(true));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean didHurt = super.hurtEnemy(stack, target, attacker);
        if (didHurt && attacker.level() instanceof ServerLevel serverLevel) {
            strikeLightning(target, serverLevel);
        }
        return didHurt;
    }

    public static void strikeLightning(LivingEntity target, ServerLevel level) {
        LightningBolt lightningBolt = new LightningBolt(net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
        lightningBolt.moveTo(target.getX(), target.getY(), target.getZ());
        lightningBolt.setVisualOnly(true);
        level.addFreshEntity(lightningBolt);

        target.hurt(target.damageSources().lightningBolt(), (float) AntarchySettings.krakensGraspLightningDamage());
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY(0.7D), target.getZ(), 10, 0.25D, 0.25D, 0.25D, 0.02D);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (!(user instanceof Player player)) {
            return;
        }

        int useTime = this.getUseDuration(stack, user) - timeLeft;
        if (useTime < 10) {
            return;
        }

        float spinStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        if (spinStrength > 0.0F && player.isInWaterOrRain()) {
            super.releaseUsing(stack, level, user, timeLeft);
            return;
        }

        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return;
        }

        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(user.getUsedItemHand()));
            KrakensGraspThrownTrident trident = new KrakensGraspThrownTrident(AntarchyObjects.KRAKENS_GRASP_TRIDENT.get(), player, level, stack.copy());
            trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
            if (player.hasInfiniteMaterials()) {
                trident.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            level.addFreshEntity(trident);
            level.playSound(null, trident, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.hasInfiniteMaterials()) {
                player.getInventory().removeItem(stack);
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private AnimatedHeldItemRenderer<KrakensGraspItem> renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (this.renderer == null) {
                    this.renderer = new AnimatedHeldItemRenderer<>(new ResourceBackedGeoItemModel<>(MODEL_LOCATION, TEXTURE_LOCATION, ANIMATION_LOCATION));
                }

                return this.renderer;
            }
        });
    }
}
