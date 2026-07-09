package com.craisinlord.apple_cows.neoforge;

import com.craisinlord.apple_cows.AppleCows;
import com.craisinlord.apple_cows.neoforge.registry.AppleCowsNeoforgeEntities;
import com.craisinlord.apple_cows.neoforge.registry.AppleCowsNeoforgeItems;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@Mod(AppleCows.MODID)
public class AppleCowsNeoforge {
    public AppleCowsNeoforge(IEventBus modEventBus) {
        AppleCowsNeoforgeEntities.register(modEventBus);
        AppleCowsNeoforgeItems.register(modEventBus);
        modEventBus.addListener(this::onSpawnPlacements);
        modEventBus.addListener(this::onEntityAttributes);
        modEventBus.addListener(this::onBuildCreativeTab);
        AppleCows.init();
    }

    private void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(AppleCowsNeoforgeEntities.APPLE_COW.get(),
                SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Cow::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(AppleCowsNeoforgeEntities.GOLDEN_APPLE_COW.get(),
                SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Cow::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(AppleCowsNeoforgeEntities.ENCHANTED_GOLDEN_APPLE_COW.get(),
                SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Cow::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(AppleCowsNeoforgeEntities.APPLE_COW.get(), Cow.createAttributes().build());
        event.put(AppleCowsNeoforgeEntities.GOLDEN_APPLE_COW.get(), Cow.createAttributes().build());
        event.put(AppleCowsNeoforgeEntities.ENCHANTED_GOLDEN_APPLE_COW.get(), Cow.createAttributes().build());
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(AppleCowsNeoforgeItems.APPLE_COW_SPAWN_EGG.get());
            event.accept(AppleCowsNeoforgeItems.GOLDEN_APPLE_COW_SPAWN_EGG.get());
            event.accept(AppleCowsNeoforgeItems.ENCHANTED_GOLDEN_APPLE_COW_SPAWN_EGG.get());
        }
    }
}
