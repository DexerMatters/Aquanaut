package com.dexer.aquanaut.client.particle;

import com.dexer.aquanaut.common.sonar.SonarMoteOptions;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

/**
 * A speck of water disturbed by a sonar pulse.
 *
 * <p>
 * The instrument's own particle, because nothing in vanilla is one. A bubble rises and pops, a spark
 * is made of magic and a puff of smoke is black and belongs in air; what a diver actually sees when
 * a pulse goes past is the water sulking — a little silt lifted and a few micro-bubbles shaken out
 * of solution, hanging, gone. So this is a soft speck with no gravity at all, most of its speed
 * spent in the first few ticks, that swells very slightly and dissolves rather than popping.
 *
 * <p>
 * The motion is left to whoever spawns it, which is what lets one particle do both of the jobs the
 * instrument has for it: given the wavefront's own outward speed it rides the shell, and given
 * almost nothing it hangs where a return struck — the same speck either way, because it is the same
 * water.
 */
public final class SonarMoteParticle extends TextureSheetParticle {

    /** How long a speck hangs, in ticks, before and after the roll below. */
    private static final int MIN_LIFETIME = 10;
    private static final int LIFETIME_SPREAD = 8;

    /**
     * How much of its speed a speck keeps each tick. Nearly all of it: the water it is suspended in
     * resists, but a mote that stopped dead on the first tick would look painted on rather than
     * carried, and one that never slowed would outrun the shell it belongs to.
     */
    private static final float FRICTION = 0.92F;

    /** How far a speck swells over its life. Barely; a mote that grew would read as a firework. */
    private static final float SWELL = 0.35F;

    private final SpriteSet sprites;
    private final float peak;
    private final float base;

    private SonarMoteParticle(ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, SonarMoteOptions options) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.lifetime = MIN_LIFETIME + this.random.nextInt(LIFETIME_SPREAD);
        this.gravity = 0.0F;
        this.friction = FRICTION;
        this.hasPhysics = false;
        this.base = 0.09F + this.random.nextFloat() * 0.05F;
        this.peak = 0.55F + this.random.nextFloat() * 0.45F;
        this.quadSize = this.base;
        this.alpha = 0.0F;
        // The colour rides on the particle; the sprite is white, so this is the whole of it.
        this.setColor(options.red(), options.green(), options.blue());
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        float t = (float) this.age / (float) this.lifetime;
        // Up quickly, held, and then a long dissolve: the water closes over it, it does not blink out.
        this.alpha = this.peak * (float) Math.sin(Math.PI * Math.min(1.0F, t * 1.12F));
        this.quadSize = this.base * (1.0F + SWELL * t);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Factory for the mote type. */
    public record Provider(SpriteSet sprites) implements ParticleProvider<SonarMoteOptions> {

        @Override
        public SonarMoteParticle createParticle(SonarMoteOptions options, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SonarMoteParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites, options);
        }
    }
}
