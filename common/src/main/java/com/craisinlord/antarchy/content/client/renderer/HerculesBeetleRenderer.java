package com.craisinlord.antarchy.content.client.renderer;

import com.craisinlord.antarchy.content.client.model.HerculesBeetleModel;
import com.craisinlord.antarchy.content.entity.HerculesBeetleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HerculesBeetleRenderer extends GeoEntityRenderer<HerculesBeetleEntity> {
    public HerculesBeetleRenderer(EntityRendererProvider.Context context) {
        super(context, new HerculesBeetleModel());
    }
}
