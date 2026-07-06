package com.craisinlord.antarchy.content.client.model;

import com.craisinlord.antarchy.Antarchy;
import com.craisinlord.antarchy.content.entity.RollyPollyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class RollyPollyModel extends GeoModel<RollyPollyEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "textures/entity/rolly_polly/rolly_polly.png");
    private static final ResourceLocation TEXTURE_SADDLED =
            ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "textures/entity/rolly_polly/rolly_polly_saddled.png");

    @Override
    public ResourceLocation getModelResource(RollyPollyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "geo/rolly_polly.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(RollyPollyEntity animatable) {
        return animatable.hasSaddle() ? TEXTURE_SADDLED : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RollyPollyEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "animations/rolly_polly.animation.json");
    }
}
