package com.dexer.aquanaut.client.sonar;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.sonar.SonarMoteOptions;
import com.dexer.aquanaut.common.sonar.SonarPulse;
import com.dexer.aquanaut.common.sonar.SonarReturn;
import com.dexer.aquanaut.common.sonar.SonarSphere;
import com.dexer.aquanaut.core.SoundRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The readings this client is holding, one per instrument that has spoken.
 *
 * <p>
 * A ping arrives as a single packet and is not consumed: the wavefront and its echoes play out over
 * the next second and a half, and then the picture <em>stays</em>. That is what a sounder does — the
 * last sweep is the one on the screen until the next one replaces it — and it is also the only
 * honest way to draw it, because the interesting part of a reading is not the flash but being able
 * to look at it while you swim. A picture is replaced when its own instrument fires again, so a
 * buddy's ping and yours can sit side by side on the dial.
 *
 * <p>
 * The contacts are kept at the world positions they were found at and nothing about them is resolved
 * against the player until the frame they are drawn in. That is what lets the whole picture stay
 * live while the diver moves and turns: bearings and ranges are recomputed from wherever they are
 * now, so a blip sits over the rock it came off rather than over the patch of water the diver
 * happened to be treading when they fired.
 *
 * <p>
 * The echoes are struck from here rather than by the server because they are positional: the whole
 * value of the instrument is hearing <em>where</em> something is, and a sound played at the
 * contact's own coordinates pans across the diver's ears as they turn. Only the strongest handful of
 * contacts are given a voice; a ping that found two dozen contacts should be a picture, not a chord.
 */
@EventBusSubscriber(modid = Aquanaut.MODID, value = Dist.CLIENT)
public final class ClientSonarData {

    /** How many of a picture's contacts are worth hearing. */
    private static final int MAX_AUDIBLE_ECHOES = 6;

    /** How many instruments' readings are kept at once, oldest dropped first. */
    private static final int MAX_PICTURES = 4;

    /**
     * How many motes are stirred up each tick the wavefront is running.
     *
     * <p>
     * Five over a twenty-six tick sweep is a little more than one dusting of the whole sphere by the
     * time the wave reaches its range, and the ones stirred up first are already further out than
     * the ones stirred up last — so the trail is a shell with a soft leading edge, the way disturbed
     * water actually looks, rather than a ring of dots all in one place.
     */
    private static final int MOTES_PER_TICK = 5;

    /** Step through the direction set between motes, coprime with its size so nothing repeats. */
    private static final int MOTE_STRIDE = 23;

    /** How many directions the wavefront is dusted along. */
    private static final int MOTE_DIRECTIONS = 128;

    /** Directions the wavefront is dusted along, fixed once so every ping dusts the same shell. */
    private static final List<Vec3> MOTES = SonarSphere.directions(MOTE_DIRECTIONS);

    /** The pictures, oldest first, keyed by the instrument that read them. */
    private static final Map<UUID, Picture> PICTURES = new LinkedHashMap<>();

    @Nullable
    private static ClientLevel lastLevel;

    private ClientSonarData() {
    }

    /** Takes delivery of a reading, replacing whatever that instrument last showed. */
    public static void accept(UUID shooter, Vec3 origin, List<SonarReturn> returns) {
        // Re-inserting moves the shooter to the end of the map, which is what makes the eviction
        // below drop the instrument that has been quiet longest rather than the one that fired first.
        PICTURES.remove(shooter);
        PICTURES.put(shooter, new Picture(origin, returns));

        while (PICTURES.size() > MAX_PICTURES) {
            PICTURES.remove(PICTURES.keySet().iterator().next());
        }
    }

    /** Every reading still on the dial, oldest first. */
    public static Collection<Picture> pictures() {
        return PICTURES.values();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level != lastLevel) {
            // A reading belongs to the water it was taken in.
            PICTURES.clear();
            lastLevel = level;
        }

        if (level == null) {
            return;
        }

        for (Picture picture : PICTURES.values()) {
            picture.tick(level);
        }
    }

    /**
     * One instrument's reading, running.
     *
     * <p>
     * The age is a float advanced once per tick and every question asked of it is asked with the
     * frame's partial tick added on, so the ring in the water moves smoothly between ticks rather
     * than stepping a block at a time. It keeps counting after the reading is complete: the picture
     * never expires on its own, but the water and the echoes still need to know how long ago it was.
     */
    public static final class Picture {

        private final Vec3 origin;
        private final List<SonarReturn> returns;
        private final boolean[] sounded;
        private final boolean[] audible;
        private float age;
        private int dusted;

        private Picture(Vec3 origin, List<SonarReturn> returns) {
            this.origin = origin;
            this.returns = List.copyOf(returns);
            this.sounded = new boolean[this.returns.size()];
            this.audible = new boolean[this.returns.size()];

            // The scan hands its contacts back strongest first, so the loudest few are at the head.
            for (int index = 0; index < Math.min(MAX_AUDIBLE_ECHOES, this.returns.size()); index++) {
                this.audible[index] = true;
            }
        }

        /** Where the pulse was fired from. */
        public Vec3 origin() {
            return this.origin;
        }

        public List<SonarReturn> returns() {
            return this.returns;
        }

        /** How old the reading is, in ticks, at this frame. */
        public float age(float partialTick) {
            return this.age + partialTick;
        }

        /** Whether the wavefront is still on its way out, or the echoes still running home. */
        public boolean isSweeping() {
            return SonarPulse.isAlive(this.age);
        }

        /** Where a contact is now, relative to the given eye. */
        public Vec3 track(SonarReturn contact, Vec3 eye) {
            return this.origin.add(contact.offset()).subtract(eye);
        }

        /** How far a contact is now, from the given eye. */
        public double range(SonarReturn contact, Vec3 eye) {
            return track(contact, eye).length();
        }

        private void tick(ClientLevel level) {
            this.age += 1.0F;

            dust(level);
            echoes(level);
        }

        /**
         * Stirs the water along the wavefront.
         *
         * <p>
         * The motes are laid down on a fixed set of directions walked in step, so the shell is
         * dusted evenly instead of speckled at random, and each one is given the wavefront's own
         * outward speed — so it travels with the pulse for as long as it lasts rather than being
         * left behind at the point it was born.
         */
        private void dust(ClientLevel level) {
            double radius = SonarPulse.wavefrontRadius(this.age);

            // Only while the front is still travelling: past its range it stops, and motes piled
            // up on a stationary shell would be a dust cloud sitting in the water for a minute.
            if (radius < 0.5D || SonarPulse.sweep(this.age) >= 1.0F) {
                return;
            }

            for (int index = 0; index < MOTES_PER_TICK; index++) {
                Vec3 direction = MOTES.get(this.dusted % MOTES.size());
                this.dusted += MOTE_STRIDE;

                Vec3 at = this.origin.add(direction.scale(radius));
                Vec3 speed = direction.scale(SonarPulse.SPEED);
                level.addParticle(SonarMoteOptions.PULSE, at.x, at.y, at.z, speed.x, speed.y, speed.z);
            }
        }

        /** Strikes the echoes that have come due, in the colour and voice the contact answered in. */
        private void echoes(ClientLevel level) {
            for (int index = 0; index < this.returns.size(); index++) {
                if (this.sounded[index] || !this.audible[index]) {
                    continue;
                }

                SonarReturn contact = this.returns.get(index);
                double distance = contact.distance();

                if (!SonarPulse.hasReached(this.age, distance)) {
                    continue;
                }

                this.sounded[index] = true;
                Vec3 at = this.origin.add(contact.offset());
                float glow = SonarPulse.echoGlow(this.age, distance, contact.strength());
                // A touch of scatter: every reflection off the same kind of surface is a little
                // different, and six echoes at one pitch sound like a machine rather than a sea.
                float scatter = 0.94F + 0.12F * ((index * 37) % 7) / 6.0F;

                level.playLocalSound(at.x, at.y, at.z, SoundRegistry.echo(contact.signal()),
                        SoundSource.PLAYERS, Mth.clamp(0.3F + 0.8F * glow, 0.08F, 1.15F), scatter, false);
                splash(level, at, contact, glow);
            }
        }

        /**
         * The little cloud of disturbed water a return leaves where it came off. One particle type
         * for every voice, with the colour carried on it: the instrument hears four different things,
         * but the sea is only ever made of water.
         */
        private static void splash(ClientLevel level, Vec3 at, SonarReturn contact, float glow) {
            RandomSource random = level.random;
            int count = 3 + (int) (6.0F * glow);
            SonarMoteOptions mote = SonarMoteOptions.of(contact.signal());

            for (int particle = 0; particle < count; particle++) {
                // Barely moving: this is water that has been pushed about, not a jet. It hangs where
                // the pulse left it and the current does the rest.
                double spread = 0.045D;
                level.addParticle(mote, at.x, at.y, at.z,
                        (random.nextDouble() - 0.5D) * spread,
                        (random.nextDouble() - 0.5D) * spread + 0.006D,
                        (random.nextDouble() - 0.5D) * spread);
            }
        }
    }
}
