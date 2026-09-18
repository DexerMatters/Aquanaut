package net.minecraft.util;

/**
 * Test-side shim for {@code net.minecraft.util.Mth}.
 *
 * <p>
 * The JUnit tests run without the Minecraft runtime, so main classes that the tests touch are linked
 * against this shim. Only the helpers used by those classes are provided, with vanilla semantics.
 */
public final class Mth {

    public static final float PI = (float) Math.PI;
    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);
    public static final float RAD_TO_DEG = (float) (180.0D / Math.PI);

    private Mth() {
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static long clamp(long value, long min, long max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    public static long floorLong(double value) {
        long truncated = (long) value;
        return value < truncated ? truncated - 1L : truncated;
    }

    public static int ceil(float value) {
        int truncated = (int) value;
        return value > truncated ? truncated + 1 : truncated;
    }

    public static int ceil(double value) {
        int truncated = (int) value;
        return value > truncated ? truncated + 1 : truncated;
    }

    public static float frac(float value) {
        return value - (float) Math.floor(value);
    }

    public static double frac(double value) {
        return value - Math.floor(value);
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static float rotLerp(float delta, float start, float end) {
        float difference = wrapDegrees(end - start);
        return start + delta * difference;
    }

    public static float wrapDegrees(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static double wrapDegrees(double value) {
        double wrapped = value % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }

    public static float approachDegrees(float delta, float start, float end) {
        float difference = wrapDegrees(end - start);
        return start + delta * clamp(difference, -delta, delta) / delta;
    }

    public static float sin(float radians) {
        return (float) Math.sin(radians);
    }

    public static float cos(float radians) {
        return (float) Math.cos(radians);
    }

    public static float sqrt(float value) {
        return (float) Math.sqrt(value);
    }

    public static double sqrt(double value) {
        return Math.sqrt(value);
    }

    public static float atan2(float y, float x) {
        return (float) Math.atan2(y, x);
    }

    public static boolean equal(float a, float b) {
        return Math.abs(b - a) < 1.0E-5F;
    }

    public static int abs(int value) {
        return Math.abs(value);
    }

    public static float abs(float value) {
        return Math.abs(value);
    }
}
