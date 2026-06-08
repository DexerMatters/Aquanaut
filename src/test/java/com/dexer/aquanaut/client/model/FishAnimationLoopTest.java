package com.dexer.aquanaut.client.model;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class FishAnimationLoopTest {

    public static void main(String[] args) {
        FishAnimationLoopTest test = new FishAnimationLoopTest();
        test.targetedSwimLoopsCloseOnTheirDeclaredLength();
    }

    private void targetedSwimLoopsCloseOnTheirDeclaredLength() {
        assertLoopClosure("creeporpedo", "swim");
        assertLoopClosure("swirl_maker", "swim");
        assertLoopClosure("lighting_worm", "swim");
    }

    private void assertLoopClosure(String animationName, String clipName) {
        String clip = loadClip(animationName, clipName);
        String endKey = extractAnimationLength(clip);
        List<String> startFrames = extractFrames(clip, "\"0.0\"");
        List<String> endFrames = extractFrames(clip, "\"" + endKey + "\"");

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
