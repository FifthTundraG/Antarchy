package com.craisinlord.antarchy.content.effect;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.content.GlimmerParticles;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class GlimmersGraceMobEffect extends MobEffect {
    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "glimmers_grace_speed");
    private static final ResourceLocation JUMP_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "glimmers_grace_jump");
    private static final ResourceLocation FALL_DAMAGE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "glimmers_grace_fall_damage");

    public GlimmersGraceMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x7FDBFF);
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributeMap, int amplifier) {
        addModifier(attributeMap, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID, 0.35D * (amplifier + 1));
        addModifier(attributeMap, Attributes.JUMP_STRENGTH, JUMP_MODIFIER_ID, 0.5D * (amplifier + 1));
        addModifier(attributeMap, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MODIFIER_ID, -0.6D * (amplifier + 1));
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeMap) {
        removeModifier(attributeMap, Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_ID);
        removeModifier(attributeMap, Attributes.JUMP_STRENGTH, JUMP_MODIFIER_ID);
        removeModifier(attributeMap, Attributes.FALL_DAMAGE_MULTIPLIER, FALL_DAMAGE_MODIFIER_ID);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        GlimmerParticles.tickAmbient(livingEntity, livingEntity.getDeltaMovement().horizontalDistanceSqr() > 0.01D);
        return true;
    }

    private static void addModifier(AttributeMap attributeMap, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = attributeMap.getInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void removeModifier(AttributeMap attributeMap, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = attributeMap.getInstance(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
    }
}
