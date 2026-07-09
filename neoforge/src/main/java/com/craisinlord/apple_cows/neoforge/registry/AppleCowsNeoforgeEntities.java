package com.craisinlord.apple_cows.neoforge.registry;

import com.craisinlord.apple_cows.AppleCows;
import com.craisinlord.apple_cows.content.entity.AppleCowEntityVariants.AppleCow;
import com.craisinlord.apple_cows.content.entity.AppleCowEntityVariants.EnchantedGoldenAppleCow;
import com.craisinlord.apple_cows.content.entity.AppleCowEntityVariants.GoldenAppleCow;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AppleCowsNeoforgeEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AppleCows.MODID);


    public static final DeferredHolder<EntityType<?>, EntityType<AppleCow>> APPLE_COW =
            ENTITY_TYPES.register("apple_cow", () -> buildCowType(AppleCow::new, "apple_cow"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoldenAppleCow>> GOLDEN_APPLE_COW =
            ENTITY_TYPES.register("golden_apple_cow", () -> buildCowType(GoldenAppleCow::new, "golden_apple_cow"));

    public static final DeferredHolder<EntityType<?>, EntityType<EnchantedGoldenAppleCow>> ENCHANTED_GOLDEN_APPLE_COW =
            ENTITY_TYPES.register("enchanted_golden_apple_cow", () -> buildCowType(EnchantedGoldenAppleCow::new, "enchanted_golden_apple_cow"));


    private AppleCowsNeoforgeEntities() {}

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }

    private static <T extends Cow> EntityType<T> buildCowType(EntityType.EntityFactory<T> factory, String name) {
        return EntityType.Builder.of(factory, MobCategory.CREATURE)
                .sized(0.9F, 1.4F)
                .clientTrackingRange(10)
                .build(name);
    }
}
