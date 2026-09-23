package com.dexer.aquanaut.client.fog;

import java.lang.reflect.Method;

/**
 * Whether a shader pack is driving the frame, asked of Iris through its public API without
 * depending on it.
 *
 * <h3>Why a pack changes what this mod must do</h3>
 * A pack that reads vanilla fog follows the mod for free: Iris's {@code fogColor},
 * {@code fogStart}, {@code fogEnd} and {@code fogShape} uniforms are filled from the very
 * fog state the mod writes. A pack that computes its own atmosphere ignores all of it, and
 * no mod can reach inside another pack's shaders to correct them — so the mod needs to know
 * which situation it is in, to decide whether to draw its own veil over the frame.
 *
 * <p>Iris is an optional dependency and this class is the whole of the coupling: the API is
 * reached reflectively, so a missing, renamed or newer Iris degrades to "no pack is driving
 * this frame" rather than to a crash or a hard dependency. The class, its singleton and its
 * method are resolved once; only the question itself is asked per frame.</p>
 */
public final class IrisCompat {
    private static final String API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";

    private static boolean resolved;
    private static Object api;
    private static Method packInUse;

    private IrisCompat() {
    }

    /** True when a shader pack is currently loaded and rendering the world. */
    public static boolean isPackInUse() {
        resolve();
        if (api == null || packInUse == null) {
            return false;
        }
        try {
            Object result = packInUse.invoke(api);
            return result instanceof Boolean inUse && inUse;
        } catch (ReflectiveOperationException | RuntimeException e) {
            forget();
            return false;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> type = Class.forName(API_CLASS);
            api = type.getMethod("getInstance").invoke(null);
            packInUse = type.getMethod("isShaderPackInUse");
        } catch (ReflectiveOperationException | RuntimeException e) {
            forget();
            resolved = true;
        }
    }

    private static void forget() {
        api = null;
        packInUse = null;
    }
}