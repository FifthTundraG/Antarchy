package com.craisinlord.apple_cows.neoforge.registry;

import com.craisinlord.apple_cows.AppleCows;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AppleCowsNeoforgeItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AppleCows.MODID);

    public static final DeferredItem<DeferredSpawnEggItem> APPLE_COW_SPAWN_EGG = ITEMS.register("apple_cow_spawn_egg",
            () -> new DeferredSpawnEggItem(AppleCowsNeoforgeEntities.APPLE_COW, 0xFF1A1A, 0x32FF32, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> GOLDEN_APPLE_COW_SPAWN_EGG = ITEMS.register("golden_apple_cow_spawn_egg",
            () -> new DeferredSpawnEggItem(AppleCowsNeoforgeEntities.GOLDEN_APPLE_COW, 0xFFE14A, 0x32FF32, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ENCHANTED_GOLDEN_APPLE_COW_SPAWN_EGG = ITEMS.register("enchanted_golden_apple_cow_spawn_egg",
            () -> new DeferredSpawnEggItem(AppleCowsNeoforgeEntities.ENCHANTED_GOLDEN_APPLE_COW, 0x7040B6, 0xFFE14A, new Item.Properties()));

    private AppleCowsNeoforgeItems() {}

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
