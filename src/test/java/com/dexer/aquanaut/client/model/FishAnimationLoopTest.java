package com.dexer.aquanaut.client.model;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Sanity checks for the looping clips that ship with the mod.
 *
 * <p>
 * The animations are exported by Blockbench itself (through the GeckoLib plugin's format), so this
 * test guards the export rather than re-deriving it: a looping clip must declare a positive length,
 * must start on a keyframe at {@code 0.0}, and must not leave a dead tail longer than one tick
 * ({@code 1/24} s) before its declared end. When the exporter does place a key exactly on the
 * declared length, the clip must also be pose-closed, i.e. end on the pose it starts with - the
 * shipped Aquanaut animations mostly are, and the few that are not are still within one tick.
 */
public final class FishAnimationLoopTest {

    /** One Minecraft tick, the granularity Blockbench snaps keyframe times to when exporting. */
    private static final double TICK = 1.0D / 24.0D;

    public static void main(String[] args) {
        FishAnimationLoopTest test = new FishAnimationLoopTest();
        test.targetedSwimLoopsCloseOnTheirDeclaredLength();
    }

    private void targetedSwimLoopsCloseOnTheirDeclaredLength() {
        assertLoopShape("creeporpedo", "swim");
        assertLoopShape("swirl_maker", "swim");
        assertLoopShape("lighting_worm", "swim");

        // Species added with models.zip.
        assertLoopShape("vamprey", "swim");
        assertLoopShape("vamprey", "charge");
        assertLoopShape("oresucker", "swim");
        assertLoopShape("flagellonautilus", "swim");
        assertLoopShape("skeleton_carp", "swim");
        assertLoopShape("golden_carp", "swim");
        assertLoopShape("silver_carp", "swim");
        assertLoopShape("gentlefish", "swim");
        assertLoopShape("slimmy", "swim");
        assertLoopShape("ionfin", "swim");
        assertLoopShape("opticichthus", "swim");
        assertLoopShape("gemini_jellyfish", "swim");
        assertLoopShape("pale_abyss_hydra", "swim");
        assertLoopShape("three_headed_shark", "swim");
        assertLoopShape("three_headed_shark", "charge");
    }

    private void assertLoopShape(String animationName, String clipName) {
        String clip = loadClip(animationName, clipName);
        String endKey = extractAnimationLength(clip);
        double length = Double.parseDouble(endKey);
        if (length <= 0.0D) {
            throw new AssertionError(animationName + "." + clipName + " declares a non-positive length");
        }

        List<Double> times = extractTimes(clip);
        if (times.isEmpty()) {
            throw new AssertionError(animationName + "." + clipName + " has no keyframes");
        }
        if (times.stream().noneMatch(time -> time == 0.0D)) {
            throw new AssertionError(animationName + "." + clipName + " has no keyframe at 0.0");
        }

        double last = times.stream().mapToDouble(Double::doubleValue).max().orElse(0.0D);
        if (Math.abs(last - length) > TICK + 1.0E-6D) {
            throw new AssertionError(animationName + "." + clipName + " ends at " + last
                    + ", more than one tick away from its declared length " + length);
        }

        // Pose closure is only enforced when the exporter actually wrote a key on the declared end.
        List<String> startFrames = extractFrames(clip, "\"0.0\"");
        List<String> endFrames = extractFrames(clip, "\"" + endKey + "\"");
        if (endFrames.isEmpty()) {
            return;
        }
        if (startFrames.size() != endFrames.size()) {
            throw new AssertionError(animationName + "." + clipName + " expected " + startFrames.size()
                    + " closing keyframes at " + endKey + " but found " + endFrames.size());
        }

        for (int i = 0; i < startFrames.size(); i++) {
            if (!startFrames.get(i).equals(endFrames.get(i))) {
                throw new AssertionError(animationName + "." + clipName
                        + " does not end on the same pose it starts with at index " + i);
            }
        }
    }

    /** Every numeric key of the clip object is a keyframe time. */
    private List<Double> extractTimes(String clip) {
        List<Double> times = new ArrayList<>();
        int index = 0;
        while (index < clip.length()) {
            int quote = clip.indexOf('"', index);
            if (quote < 0) {
                break;
            }
            int close = clip.indexOf('"', quote + 1);
            if (close < 0) {
                break;
            }
            String key = clip.substring(quote + 1, close);
            if (key.matches("\\d+(\\.\\d+)?")) {
                times.add(Double.parseDouble(key));
            }
            index = close + 1;
        }
        return times;
    }

    private List<String> extractFrames(String clip, String key) {
        List<String> frames = new ArrayList<>();
        int index = 0;
        while ((index = clip.indexOf(key, index)) >= 0) {
            int objectStart = clip.indexOf('{', index);
            if (objectStart < 0) {
                break;
            }
            frames.add(extractObject(clip, objectStart));
            index = objectStart + 1;
        }
        return frames;
    }

    private String loadClip(String animationName, String clipName) {
        String path = "assets/aquanaut/animations/" + animationName + ".animation.json";
        try (InputStream stream = FishAnimationLoopTest.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new AssertionError("missing animation resource " + path);
            }

            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            int clipKey = json.indexOf("\"" + clipName + "\"");
            if (clipKey < 0) {
                throw new AssertionError("missing clip " + clipName + " in " + path);
            }
            int objectStart = json.indexOf('{', clipKey);
            if (objectStart < 0) {
                throw new AssertionError("missing object body for clip " + clipName + " in " + path);
            }
            return extractObject(json, objectStart);
        } catch (Exception e) {
            throw new AssertionError("failed to load animation resource " + path, e);
        }
    }

    private String extractObject(String text, int objectStart) {
        int depth = 0;
        for (int i = objectStart; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(objectStart, i + 1);
                }
            }
        }
        throw new AssertionError("unterminated object in animation data");
    }

    private String extractAnimationLength(String clip) {
        String key = "\"animation_length\":";
        int start = clip.indexOf(key);
        if (start < 0) {
            throw new AssertionError("missing animation_length in clip");
        }
        start += key.length();
        int end = start;
        while (end < clip.length()) {
            char c = clip.charAt(end);
            if ((c >= '0' && c <= '9') || c == '.') {
                end++;
                continue;
            }
            if (end > start) {
                return clip.substring(start, end).trim();
            }
            end++;
        }
        throw new AssertionError("unterminated animation_length in clip");
    }
}
