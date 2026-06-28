package com.craisinlord.antarchy.content.fluid;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.content.AntarchyObjects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

public class BileLiquidBlock extends LiquidBlock {
    private static final ResourceLocation BILE_ID = ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "bile");
    private static final ResourceLocation FLOWING_BILE_ID = ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "flowing_bile");
    private static final int EFFECT_DURATION_TICKS = 100;

    public BileLiquidBlock(FlowingFluid fluid, BlockBehaviour.Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        if (entity instanceof LivingEntity livingEntity) {
            if (!level.isClientSide) {
                livingEntity.addEffect(new MobEffectInstance(AntarchyObjects.STINKY_EFFECT.get(), EFFECT_DURATION_TICKS, 0, false, true, true));
                livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_DURATION_TICKS, 0, false, true, true));
            }

            if (livingEntity.getType().is(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
                return;
            }
        }
    }

    public static boolean isBile(FluidState fluidState) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluidState.getType());
        return BILE_ID.equals(fluidId) || FLOWING_BILE_ID.equals(fluidId);
    }
}
