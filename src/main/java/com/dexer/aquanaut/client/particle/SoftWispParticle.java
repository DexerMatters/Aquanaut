package com.dexer.aquanaut.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A soft, slowly turning vapor wisp. Shared by the caldera's gases and ash: one churning
 * translucent puff that breathes in, drifts and dissolves.
 */
public final class SoftWispParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseAlpha;
    private final float baseSize;

    private SoftWispParticle(ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed,
            SpriteSet sprites, float red, float green, float blue,
            float size, int lifetime, float gravity, float alpha) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.baseSize = size;
        this.baseAlpha = alpha;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.gravity = gravity;
        this.lifetime = lifetime;
        this.quadSize = size;
        this.setColor(red, green, blue);
        this.alpha = 0.0F;
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(sprites);
        // Breathe: fade in fast, dissolve slow, while the wisp swells a little.
        float t = (float) this.age / (float) this.lifetime;
        this.alpha = baseAlpha * (float) Math.sin(Math.PI * Math.min(1.0F, t * 1.25F));
        this.quadSize = baseSize * (1.0F + t * 0.45F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Factory keeping the sprite set and look of one wisp kind. */
    public record Provider(SpriteSet sprites, float red, float green, float blue,
            float size, int lifetime, float gravity, float alpha)
            implements ParticleProvider<SimpleParticleType> {

        @Override
        public SoftWispParticle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new SoftWispParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    sprites, red, green, blue, size, lifetime, gravity, alpha);
        }
    }
}
