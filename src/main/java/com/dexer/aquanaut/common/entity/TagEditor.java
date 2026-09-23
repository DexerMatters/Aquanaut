package com.dexer.aquanaut.common.entity;

/**
 * The seam between a taggable entity and its editor screen.
 *
 * <p>
 * {@link AbstractTaggableEntity} lives in {@code common} and runs on both sides; the editor is a
 * client-only {@code Screen}. Rather than have the entity reference a client class and rely on the
 * server never resolving it, the client installs an opener here during client setup and the entity
 * just calls {@link #open}. On a dedicated server the no-op stays in place, so nothing client-only
 * is ever named from common code.
 */
public final class TagEditor {

    /** Opens the editor for a taggable entity. Client-only in practice. */
    @FunctionalInterface
    public interface Opener {
        void open(AbstractTaggableEntity entity);
    }

    private static Opener opener = entity -> {
    };

    private TagEditor() {
    }

    /** Installed once by the client during setup. */
    public static void install(Opener value) {
        opener = value == null ? entity -> {
        } : value;
    }

    public static void open(AbstractTaggableEntity entity) {
        opener.open(entity);
    }
}
