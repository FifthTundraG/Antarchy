package com.craisinlord.antarchy.content.client.model;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.content.entity.GlimmerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class GlimmerModel extends GeoModel<GlimmerEntity> {
    private static final ResourceLocation ADULT_GEO = rl("geo/glimmer.geo.json");
    private static final ResourceLocation BABY_GEO = rl("geo/glimmer_baby.geo.json");
    private static final ResourceLocation ADULT_ANIM = rl("animations/glimmer.animation.json");
    private static final ResourceLocation BABY_ANIM = rl("animations/glimmer_baby.animation.json");
    private static final ResourceLocation ADULT_DAY = rl("textures/entity/glimmer/glimmer_day.png");
    private static final ResourceLocation ADULT_NIGHT = rl("textures/entity/glimmer/glimmer.png");
    private static final ResourceLocation BABY_DAY = rl("textures/entity/glimmer/glimmer_baby_day.png");
    private static final ResourceLocation BABY_NIGHT = rl("textures/entity/glimmer/glimmer_baby.png");
    public static final ResourceLocation ADULT_EMISSIVE = rl("textures/entity/glimmer/glimmer_emissive.png");
    public static final ResourceLocation BABY_EMISSIVE = rl("textures/entity/glimmer/glimmer_baby_emissive.png");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, path);
    }

    @Override
    public ResourceLocation getModelResource(GlimmerEntity animatable) {
        return animatable.isBaby() ? BABY_GEO : ADULT_GEO;
    }

    @Override
    public ResourceLocation getAnimationResource(GlimmerEntity animatable) {
        return animatable.isBaby() ? BABY_ANIM : ADULT_ANIM;
    }

    @Override
    public ResourceLocation getTextureResource(GlimmerEntity animatable) {
        return textureFor(animatable, animatable.getGlowBlend(0.0F) >= 0.5F);
    }

    public static ResourceLocation textureFor(GlimmerEntity animatable, boolean glowing) {
        if (animatable.isBaby()) {
            return glowing ? BABY_NIGHT : BABY_DAY;
        }
        return glowing ? ADULT_NIGHT : ADULT_DAY;
    }
}
