package com.dexer.aquanaut.client.drone;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Takes a shader pack out of the world render for the length of one pass, and puts it back.
 *
 * <h3>Why the feed cannot share the pack's frame</h3>
 * A shader pack does not decorate a frame, it owns it. Its pipeline is created per dimension and then
 * driven per frame: g-buffers bound, deferred passes run, history accumulated for the temporal
 * filters. None of that has any notion of a second camera, and all of it is frame-scoped state.
 *
 * <p>
 * A nested world render through it is therefore not merely slower, it is wrong. The pack is handed the
 * drone's camera and records it as the frame it just drew, which is what the next frame's history is
 * reprojected against — the player's whole screen flickers between the two cameras, and everything that
 * accumulates smears. That is the failure this class prevents, and it is not specific to one pack: any
 * renderer holding state across a frame has it.
 *
 * <h3>What it does</h3>
 * For the pass, the world render is pointed at a pipeline that does nothing — Iris's own vanilla
 * pipeline, the one it uses when no pack is loaded. The world then renders exactly as it would with no
 * pack at all: vanilla and Sodium chunk programs, no g-buffers, no composite, no history touched. The
 * pack is put back unchanged afterwards.
 *
 * <p>
 * This is the one place the feed knows about a specific renderer, and deliberately the only one:
 * everything else about the pass is isolated on its own terms — its own buffer, its own framing, its
 * own clock — and needs nobody's cooperation. There is no general interface for "stand aside for this
 * pass", so the integration goes through Iris's public entry points plus one private map, and it
 * degrades honestly: when a pack is loaded and cannot be stood down, {@link #quiet()} says so and the
 * feed does not render at all, rather than driving the pack a second time.
 */
final class ShaderStanddown {

    // ── how the integration turned out ────────────────────────────────────────
    private static final int UNKNOWN = 0;
    /** No Iris at all: there is nothing to stand down and the pass is already quiet. */
    private static final int NONETODO = 1;
    /** Iris is here and the hooks resolved: {@link #install()} can be used. */
    private static final int READY = 2;
    /** A pack is loaded and cannot be stood down. The feed must not render. */
    private static final int BROKEN = 3;

    /**
     * The settings the pack owns, as getter/setter pairs. Iris's vanilla pipeline resets every one of
     * them in its constructor, and the pack set them when it was created — constructing that pipeline
     * without putting them back would leave the pack's own rendering configured for a vanilla frame.
     */
    private static final String[][] SETTINGS = {
            { "shouldDisableDirectionalShading", "setDisableDirectionalShading" },
            { "shouldUseSeparateAo", "setUseSeparateAo" },
            { "shouldSeparateEntityDraws", "setSeparateEntityDraws" },
            { "getAmbientOcclusionLevel", "setAmbientOcclusionLevel" },
            { "getVertexFormat", "setVertexFormat" },
            { "shouldVoxelizeLightBlocks", "setVoxelizeLightBlocks" },
            { "getBlockTypeIds", "setBlockTypeIds" },
    };

    private static int state = UNKNOWN;
    @Nullable
    private static Throwable problem;

    @Nullable
    private static Method pipelineManager;
    @Nullable
    private static Method currentDimension;
    @Nullable
    private static Field perDimension;
    @Nullable
    private static Object settings;
    @Nullable
    private static Method[] getters;
    @Nullable
    private static Method[] setters;
    @Nullable
    private static Object[] values;
    /** The pipeline that does nothing, standing in for the pack's while the pass runs. */
    @Nullable
    private static Object quiet;

    /** What the pass displaced, so it can be put back exactly. */
    private static boolean standing;
    private static boolean hadEntry;
    @Nullable
    private static Object displacedKey;
    @Nullable
    private static Object displaced;

    private ShaderStanddown() {
    }

    /** Why the integration failed, for a log line; {@code null} when it did not. */
    @Nullable
    static Throwable failure() {
        return problem;
    }

    /**
     * Makes the world render quiet for one pass.
     *
     * @return whether the pass may render: true when there is no pack to stand down or the pack was
     *         stood down, false when a pack is loaded and could not be, in which case the caller must
     *         not render the world.
     */
    static boolean quiet() {
        if (state == UNKNOWN) {
            resolve();
        }
        return switch (state) {
            case NONETODO -> true;
            case READY -> install();
            default -> false;
        };
    }

    /** Puts the pack back, exactly as it was found. */
    static void resume() {
        if (!standing) {
            return;
        }
        standing = false;
        try {
            Map<Object, Object> map = perDimensionOf();
            if (hadEntry) {
                map.put(displacedKey, displaced);
            } else {
                map.remove(displacedKey);
            }
            restoreSettings();
        } catch (Throwable failure) {
            problem = failure;
            state = BROKEN;
        }
    }

    // ------------------------------------------------------------------
    // the integration
    // ------------------------------------------------------------------

    private static boolean install() {
        try {
            // The reflective probe is kept even though its result is not: reaching the
            // accessor is what forces the pipeline class to initialise.
            pipelineManager.invoke(null);
            displacedKey = currentDimension.invoke(null);
            Map<Object, Object> map = perDimensionOf();
            hadEntry = map.containsKey(displacedKey);
            displaced = map.get(displacedKey);
            map.put(displacedKey, quiet);
            standing = true;
            return true;
        } catch (Throwable failure) {
            problem = failure;
            state = BROKEN;
            return false;
        }
    }

    /**
     * Resolves the hooks once.
     *
     * <p>
     * Only Iris's public entry points are called reflectively; the one private member, the map of
     * pipelines per dimension, is the lever that decides which pipeline the next world render gets, and
     * there is no public way to reach it. Iris ships as an automatic module, so this all works from a
     * mod — but a future version could move any of it, which is what {@link #BROKEN} is for.
     */
    private static void resolve() {
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Class<?> managerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
            Class<?> quietClass = Class.forName("net.irisshaders.iris.pipeline.VanillaRenderingPipeline");

            pipelineManager = iris.getMethod("getPipelineManager");
            currentDimension = iris.getMethod("getCurrentDimension");
            perDimension = managerClass.getDeclaredField("pipelinesPerDimension");
            perDimension.setAccessible(true);

            resolveSettings();

            // Constructing it resets the pack's settings, so they are captured either side of it.
            quiet = quietClass.getConstructor().newInstance();
            restoreSettings();
            state = READY;
        } catch (ClassNotFoundException absent) {
            // Not installed. Nothing owns the world render, so there is nothing to stand down.
            state = NONETODO;
        } catch (Throwable failure) {
            problem = failure;
            state = BROKEN;
        }
    }

    private static void resolveSettings() throws ReflectiveOperationException {
        settings = Class.forName("net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings")
                .getField("INSTANCE")
                .get(null);
        Class<?> type = settings.getClass();
        getters = new Method[SETTINGS.length];
        setters = new Method[SETTINGS.length];
        values = new Object[SETTINGS.length];
        for (int i = 0; i < SETTINGS.length; i++) {
            getters[i] = type.getMethod(SETTINGS[i][0]);
            setters[i] = type.getMethod(SETTINGS[i][1], getters[i].getReturnType());
            values[i] = getters[i].invoke(settings);
        }
    }

    private static void restoreSettings() throws ReflectiveOperationException {
        if (settings == null || setters == null || values == null) {
            return;
        }
        for (int i = 0; i < SETTINGS.length; i++) {
            setters[i].invoke(settings, values[i]);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> perDimensionOf() throws ReflectiveOperationException {
        return (Map<Object, Object>) perDimension.get(pipelineManager.invoke(null));
    }
}