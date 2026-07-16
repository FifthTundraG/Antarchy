package com.craisinlord.antarchy.content.client.model;

import com.craisinlord.antarchy.content.entity.kraken.KrakensGraspThrownTrident;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KrakensGraspThrownTridentModel extends GeoModel<KrakensGraspThrownTrident> {
    @Override
    public ResourceLocation getModelResource(KrakensGraspThrownTrident animatable) {
        return ResourceLocation.fromNamespaceAndPath("antarchy", "geo/krakens_grasp.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KrakensGraspThrownTrident animatable) {
        return ResourceLocation.fromNamespaceAndPath("antarchy", "textures/item/krakens_grasp/krakens_grasp.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KrakensGraspThrownTrident animatable) {
        return ResourceLocation.fromNamespaceAndPath("antarchy", "animations/static_item.animation.json");
    }
}
