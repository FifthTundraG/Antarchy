package com.craisinlord.antarchy.fabric.client;

import com.craisinlord.antarchy.Antarchy;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class AntarchyKeyBindings {
    public static final String CATEGORY = "key.categories.antarchy";

    public static final KeyMapping BRUTALFLY_FLAP = new KeyMapping(
            "key.antarchy.brutalfly_flap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    public static final KeyMapping DORRIE_CHARGE_JUMP = new KeyMapping(
            "key.antarchy.dorrie_charge_jump",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY
    );

    private AntarchyKeyBindings() {}

    public static void register() {
        KeyBindingHelper.registerKeyBinding(BRUTALFLY_FLAP);
        KeyBindingHelper.registerKeyBinding(DORRIE_CHARGE_JUMP);
    }

    public static boolean isBrutalflyFlapPressed() {
        return Minecraft.getInstance().screen == null && BRUTALFLY_FLAP.isDown();
    }

    public static boolean isDorrieChargeJumpPressed() {
        return Minecraft.getInstance().screen == null && DORRIE_CHARGE_JUMP.isDown();
    }
}
