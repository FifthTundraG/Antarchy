package com.craisinlord.apple_cows.fabric;

import com.craisinlord.apple_cows.AppleCows;
import com.craisinlord.apple_cows.content.entity.AppleCowEntityVariants.AppleCow;
import com.craisinlord.apple_cows.content.entity.AppleCowEntityVariants.EnchantedGoldenAppleCow;
import com.craisinlord.apple_cows.content.entity.AppleCowEntityVariants.GoldenAppleCow;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

public final class AppleCowsFabricContent {
    public static EntityType<AppleCow> APPLE_COW;
    public static EntityType<GoldenAppleCow> GOLDEN_APPLE_COW;
    public static EntityType<EnchantedGoldenAppleCow> ENCHANTED_GOLDEN_APPLE_COW;

    public static Item APPLE_COW_SPAWN_EGG;
    public static Item GOLDEN_APPLE_COW_SPAWN_EGG;
    public static Item ENCHANTED_GOLDEN_APPLE_COW_SPAWN_EGG;

    public static void register() {
        APPLE_COW = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(AppleCows.MODID, "apple_cow"),
                EntityType.Builder.of(AppleCow::new, MobCategory.CREATURE)
                        .sized(0.9F, 1.4F).clientTrackingRange(10).build("apple_cow"));

        GOLDEN_APPLE_COW = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(AppleCows.MODID, "golden_apple_cow"),
                EntityType.Builder.of(GoldenAppleCow::new, MobCategory.CREATURE)
                        .sized(0.9F, 1.4F).clientTrackingRange(10).build("golden_apple_cow"));

        ENCHANTED_GOLDEN_APPLE_COW = Registry.register(BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(AppleCows.MODID, "enchanted_golden_apple_cow"),
                EntityType.Builder.of(EnchantedGoldenAppleCow::new, MobCategory.CREATURE)
                        .sized(0.9F, 1.4F).clientTrackingRange(10).build("enchanted_golden_apple_cow"));

        FabricDefaultAttributeRegistry.register(APPLE_COW, Cow.createAttributes());
        FabricDefaultAttributeRegistry.register(GOLDEN_APPLE_COW, Cow.createAttributes());
        FabricDefaultAttributeRegistry.register(ENCHANTED_GOLDEN_APPLE_COW, Cow.createAttributes());

        SpawnPlacements.register(APPLE_COW, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(GOLDEN_APPLE_COW, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);
        SpawnPlacements.register(ENCHANTED_GOLDEN_APPLE_COW, SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules);

        APPLE_COW_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(AppleCows.MODID, "apple_cow_spawn_egg"),
                new SpawnEggItem(APPLE_COW, 0xFF1A1A, 0x32FF32, new Item.Properties()));

        GOLDEN_APPLE_COW_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(AppleCows.MODID, "golden_apple_cow_spawn_egg"),
                new SpawnEggItem(GOLDEN_APPLE_COW, 0xFFE14A, 0x32FF32, new Item.Properties()));

        ENCHANTED_GOLDEN_APPLE_COW_SPAWN_EGG = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(AppleCows.MODID, "enchanted_golden_apple_cow_spawn_egg"),
                new SpawnEggItem(ENCHANTED_GOLDEN_APPLE_COW, 0x7040B6, 0xFFE14A, new Item.Properties()));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(APPLE_COW_SPAWN_EGG.getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            entries.accept(GOLDEN_APPLE_COW_SPAWN_EGG.getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            entries.accept(ENCHANTED_GOLDEN_APPLE_COW_SPAWN_EGG.getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
    }
}
