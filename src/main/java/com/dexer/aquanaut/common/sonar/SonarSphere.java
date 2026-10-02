package com.dexer.aquanaut.common.sonar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.world.phys.Vec3;

/**
 * The two pieces of the scan that are arithmetic rather than world-reading: which directions the
 * pulse is listened in, and how the rays that came back are folded into contacts.
 *
 * <p>
 * Both live apart from {@link SonarScan} so that they can be checked without a level. They are also
 * the two places where the instrument's character is decided — how evenly it listens, and how
 * eagerly it merges what it hears — so they are the two places a change is most likely to be felt
 * everywhere at once.
 *
 * <p>
 * Deliberately free of Minecraft beyond the vector type, which the test source set shims.
 */
public final class SonarSphere {

    /**
     * The golden angle, in radians. Spreading directions by this much on each step is what keeps a
     * spiral of points on a sphere from lining up into seams: successive turns never repeat a
     * longitude, so no direction is ever listened in twice while another is missed.
     */
    private static final double GOLDEN_ANGLE = Math.PI * (1.0D + Math.sqrt(5.0D));

    private SonarSphere() {
    }

    /**
     * {@code count} directions spread as evenly as a sphere allows, each a unit vector. The set is
     * deterministic — no seed, no randomness — so a ping fired twice from the same spot reads the
     * same world the same way, and the scan can be reasoned about at all.
     */
    public static List<Vec3> directions(int count) {
        List<Vec3> directions = new ArrayList<>(Math.max(0, count));

        for (int index = 0; index < count; index++) {
            // Band by band from the north pole down, each band holding exactly one point.
            double y = 1.0D - 2.0D * (index + 0.5D) / count;
            double ring = Math.sqrt(Math.max(0.0D, 1.0D - y * y));
            double azimuth = GOLDEN_ANGLE * index;
            directions.add(new Vec3(Math.cos(azimuth) * ring, y, Math.sin(azimuth) * ring));
        }

        return directions;
    }

    /**
     * Folds rays that found the same thing into one contact each, strongest first and capped at
     * {@code max}.
     *
     * <p>
     * A wall of ore two blocks across answers dozens of rays and is one vein; a shoal of six fish is
     * six contacts the diver may want to tell apart. The radius a voice merges over is therefore the
     * voice's own — see {@link SonarSignal#cluster} — rather than one number for everything, and the
     * strongest ray of a group is the one whose direction and depth are reported, because that is
     * the one the instrument actually heard best.
     */
    public static List<SonarReturn> cluster(List<SonarReturn> rays, int max) {
        List<SonarReturn> sorted = new ArrayList<>(rays);
        sorted.sort(Comparator.comparingDouble((SonarReturn contact) -> contact.strength()).reversed());

        List<SonarReturn> contacts = new ArrayList<>();

        for (SonarReturn ray : sorted) {
            boolean merged = false;

            for (int index = 0; index < contacts.size(); index++) {
                SonarReturn contact = contacts.get(index);
                double reach = Math.max(contact.signal().cluster, ray.signal().cluster);

                if (contact.signal() == ray.signal() && within(contact.offset(), ray.offset(), reach)) {
                    contacts.set(index, new SonarReturn(contact.signal(), contact.offset(),
                            Math.max(contact.strength(), ray.strength()), contact.count() + ray.count()));
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                if (contacts.size() >= max) {
                    // Everything left is weaker than everything kept, and the list is strongest
                    // first, so the cap can simply stop reading.
                    break;
                }
                contacts.add(ray);
            }
        }

        return contacts;
    }

    private static boolean within(Vec3 one, Vec3 other, double reach) {
        return one.subtract(other).lengthSqr() <= reach * reach;
    }
}
