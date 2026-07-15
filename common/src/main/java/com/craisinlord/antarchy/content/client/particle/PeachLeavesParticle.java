package com.craisinlord.antarchy.content.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public final class PeachLeavesParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float gravity;

    private PeachLeavesParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;
        this.gravity = 0.02F + this.random.nextFloat() * 0.01F;
        this.lifetime = 28 + this.random.nextInt(26);
        this.quadSize = 0.14F + this.random.nextFloat() * 0.05F;
        this.xd = (this.random.nextDouble() - 0.5D) * 0.04D;
        this.yd = -0.015D - this.random.nextDouble() * 0.02D;
        this.zd = (this.random.nextDouble() - 0.5D) * 0.04D;
        this.hasPhysics = true;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        double sway = Math.sin((this.age + this.x) * 0.22D) * 0.0035D;
        this.xd += sway;
        this.zd -= sway;
        this.yd -= this.gravity * 0.015D;

        this.xd *= 0.91D;
        this.yd *= 0.92D;
        this.zd *= 0.91D;

        this.move(this.xd, this.yd, this.zd);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new PeachLeavesParticle(level, x, y, z, this.sprites);
        }
    }
}
