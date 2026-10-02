package com.dexer.aquanaut.common.sonar;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Where the pulse is followed once a wall has stopped it.
 *
 * <p>
 * Straight in through the face it struck, not along the ray that struck it. That distinction is the
 * whole of this class's correctness. Marching along the ray looks equivalent and is not: a ray that
 * meets a wall at a glancing angle travels mostly <em>alongside</em> the wall, so half a block
 * further on it steps sideways out of the rock and into the water it came from — and then every
 * shallow hit on a distant wall reads as a hollow, which is what a sonar reporting the diver's own
 * surroundings back at them looks like. Following the face's own inward normal cannot do that: the
 * first step is inside the block that stopped the pulse, and the way out is eight blocks of rock
 * away.
 *
 * <p>
 * It is also what sound does. A pulse that arrives at a wall goes into it, and what comes back off
 * the far side of a few blocks of rock is a hollow, wherever the diver happened to be standing when
 * they fired.
 *
 * <p>
 * Deliberately free of the level and of everything else, so the one rule that decides whether the
 * instrument finds caves or finds the water it is floating in can be checked by a plain unit test.
 */
public final class SonarCavity {

    /**
     * How far past a face the pulse is followed, in blocks. Deep enough to reach the caves that
     * thread the sea floor, shallow enough that the far side of a ridge is not reported as a hollow
     * in the near side of it.
     */
    public static final int DEPTH = 8;

    private SonarCavity() {
    }

    /**
     * The blocks to look at, in order, after {@code face} stopped the pulse. {@code struckFace} is
     * the face the pulse arrived at — vanilla's {@code BlockHitResult.getDirection()} — and the
     * probes run the other way, into the block and out the far side.
     */
    public static List<BlockPos> probes(BlockPos face, Direction struckFace) {
        Direction into = struckFace.getOpposite();
        List<BlockPos> probes = new ArrayList<>(DEPTH);

        for (int step = 1; step <= DEPTH; step++) {
            probes.add(face.relative(into, step));
        }

        return probes;
    }
}
