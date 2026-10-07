package com.dexer.aquanaut.common.worldgen.layers;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the reef ceiling's district borders: the coral, jelly and mud provinces share the band,
 * so their weights must cross over a wide, smooth band rather than flipping at a contour. A hard
 * flip is what the world-gen report describes as an abrupt border.
 */
public final class ReefTransitionTest {
    @Test
    void reefDistrictsCrossOverAWideBand() throws Exception {
        OceanLayerStack stack = OceanLayerStacks.active();
        OceanLayer reef = stack.layers().stream()
                .filter(layer -> layer.id().getPath().equals("reef_ceiling"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no reef_ceiling layer in the active stack"));

        BiomeMix mix = reef.mix();
        double previousFirst = mix.weightsAt(0, 0)[0];

        double maxStep = 0.0D;
        int overlapQuarts = 0;
        int flips = 0;
        int lastWinner = -1;

        // The mix cells are 32 quarts wide, so this line crosses many districts.
        int lineQuarts = 4096;
        for (int quart = 0; quart < lineQuarts; quart++) {
            double[] weights = mix.weightsAt(quart, 0);

            maxStep = Math.max(maxStep, Math.abs(weights[0] - previousFirst));
            previousFirst = weights[0];

            double best = 0.0D;
            double second = 0.0D;
            int winner = 0;
            for (int i = 0; i < weights.length; i++) {
                if (weights[i] > best) {
                    second = best;
                    best = weights[i];
                    winner = i;
                } else if (weights[i] > second) {
                    second = weights[i];
                }
            }
            if (second >= 0.25D) {
                overlapQuarts++;
            }
            if (lastWinner >= 0 && winner != lastWinner) {
                flips++;
            }
            lastWinner = winner;
        }

        String report = "maxStepPerQuart=" + String.format("%.4f", maxStep)
                + " overlapQuarts=" + overlapQuarts
                + " of " + lineQuarts
                + " flips=" + flips;
        Files.createDirectories(Path.of("build"));
        Files.writeString(Path.of("build", "reef-transition-measure.txt"), report);

        // A contour flip would step the weight by a large fraction in a single quart (4 blocks).
        // The smooth affinity path keeps every step tiny.
        assertTrue(maxStep < 0.10D, "reef district weights step too sharply: " + report);
        // And the two leading districts must be comparable over a broad band, not a seam.
        assertTrue(overlapQuarts >= lineQuarts / 4,
                "reef district overlap band too narrow: " + report);
    }
}
