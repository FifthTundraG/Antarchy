package com.craisinlord.antarchy.fabric;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.fabric.mixin.WoodTypeInvoker;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public final class OuranwoodWoodTypes {
    public static final WoodType OURANWOOD = register("ouranwood");

    private OuranwoodWoodTypes() {
    }

    private static WoodType register(String name) {
        return WoodTypeInvoker.antarchy$register(new WoodType(Antarchy.MODID + ":" + name, BlockSetType.JUNGLE));
    }
}
