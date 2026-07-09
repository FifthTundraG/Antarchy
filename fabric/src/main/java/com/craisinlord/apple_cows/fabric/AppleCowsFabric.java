package com.craisinlord.apple_cows.fabric;

import com.craisinlord.apple_cows.AppleCows;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

public final class AppleCowsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AppleCowsFabricContent.register();
        registerSpawns();
        AppleCows.init();
    }

    private static void registerSpawns() {
        addCowSpawns("apple_cow_spawn_biomes", AppleCowsFabricContent.APPLE_COW, 23, 2, 4);
        addCowSpawns("golden_apple_cow_spawn_biomes", AppleCowsFabricContent.GOLDEN_APPLE_COW, 5, 1, 2);
        addCowSpawns("enchanted_golden_apple_cow_spawn_biomes", AppleCowsFabricContent.ENCHANTED_GOLDEN_APPLE_COW, 1, 1, 1);
    }

    private static <T extends net.minecraft.world.entity.Entity> void addCowSpawns(
            String biomeTagPath, net.minecraft.world.entity.EntityType<T> entityType,
            int weight, int minCount, int maxCount) {
        TagKey<Biome> tag = TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(AppleCows.MODID, biomeTagPath));
        BiomeModifications.addSpawn(BiomeSelectors.tag(tag), MobCategory.CREATURE, entityType, weight, minCount, maxCount);
    }
}
