package com.craisinlord.antarchy.content.client.renderer;

import com.craisinlord.antarchy.content.client.model.KrakensGraspThrownTridentModel;
import com.craisinlord.antarchy.content.entity.kraken.KrakensGraspThrownTrident;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class KrakensGraspThrownTridentRenderer extends GeoEntityRenderer<KrakensGraspThrownTrident> {
    public KrakensGraspThrownTridentRenderer(EntityRendererProvider.Context context) {
        super(context, new KrakensGraspThrownTridentModel());
        this.shadowRadius = 0.0F;
    }
}
