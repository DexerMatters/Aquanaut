package com.dexer.aquanaut.common.sonar;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * One contact: a direction the pulse came back from, how far away it was, and how much of the pulse
 * it threw back.
 *
 * <p>
 * The offset is kept as a vector rather than as a bearing and a range because every consumer wants
 * a different pair out of it — the world effect wants the point itself, the scope wants the bearing
 * and the radius, the readout wants the distance and the depth — and one vector answers all three
 * without anybody re-deriving an angle that was already thrown away.
 *
 * <p>
 * {@code count} is how many of the scan's rays agreed on this contact. It is what turns a single
 * ore block into a "small vein" and a long cavern wall into a strong one, and it is carried across
 * the wire because the readout says so on the client.
 */
public record SonarReturn(SonarSignal signal, Vec3 offset, float strength, int count) {

    public SonarReturn {
        signal = signal == null ? SonarSignal.BIOLOGICAL : signal;
        offset = offset == null ? Vec3.ZERO : offset;
        strength = Mth.clamp(strength, 0.0F, 1.0F);
        count = Math.max(1, count);
    }

    public static SonarReturn at(SonarSignal signal, Vec3 offset, float strength) {
        return new SonarReturn(signal, offset, strength, 1);
    }

    /** How far the contact is from the transducer, in blocks. */
    public double distance() {
        return this.offset.length();
    }

}
