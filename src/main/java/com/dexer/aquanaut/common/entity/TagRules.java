package com.dexer.aquanaut.common.entity;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * The naming and colour rules for a {@link AbstractTaggableEntity}'s tag.
 *
 * <p>
 * A tag is <em>both</em> the label and the colour it is drawn in. A freshly placed taggable is named
 * {@code #N}, where {@code N} is the smallest positive integer no other one is using, and gets a
 * random colour; the player can then rename and recolour it from the tag editor. Both paths run
 * through the same uniqueness rule, so <b>duplicate tags cannot exist</b> — placement picks an
 * unclaimed number and the editor refuses a name somebody else already wears.
 *
 * <p>
 * Uniqueness is global rather than per-kind: a cursor and a drone share one namespace, because the
 * whole point of a tag is to be unambiguous when the compass or a notebook entry names it.
 *
 * <p>
 * Deliberately free of Minecraft types so the rules can be unit tested without the game runtime.
 */
public final class TagRules {

    /** Longest tag the editor accepts, in characters. */
    public static final int MAX_NAME_LENGTH = 16;

    /**
     * Tag colours. Chosen to stay legible as a nameplate against the dark underwater palette, and
     * all fully opaque so the renderer never has to blend them.
     */
    public static final int[] TAG_COLORS = {
            0xE8705C, // coral
            0xE8A020, // amber
            0xF2D14E, // brass
            0x7FD46A, // kelp
            0x4FD0C0, // lagoon
            0x54A8F0, // shoal
            0x8A7CF0, // abyss
            0xD46AD0, // anemone
            0xF08AB0, // blush
            0xE8EFED, // foam
            0xB4C2C0, // shell
            0x6ED0F0, // ice
    };

    /**
     * The one thing this class needs from a random source. Narrower than either
     * {@code java.util.random.RandomGenerator} or Minecraft's {@code RandomSource}, so an entity can
     * pass {@code level.random::nextInt} and a test can pass {@code java.util.Random::nextInt}.
     */
    @FunctionalInterface
    public interface BoundPicker {
        /** @return a value in {@code [0, bound)} */
        int nextInt(int bound);
    }

    private TagRules() {
    }

    /** The automatic label for a number: {@code 7} becomes {@code "#7"}. */
    public static String format(int index) {
        return "#" + index;
    }

    /**
     * The lowest positive number that no other taggable has claimed as a name.
     *
     * @param takenName tests whether a candidate label is already worn by another taggable
     */
    public static String firstFreeName(Predicate<String> takenName) {
        return format(firstFreeIndex(index -> takenName.test(format(index)), 0));
    }

    /**
     * The smallest positive index that is neither taken nor equal to {@code currentIndex}.
     *
     * @param taken        tests whether an index is already held by a <em>different</em> taggable
     * @param currentIndex the index this taggable holds now, or {@code 0} when it has none yet
     */
    public static int firstFreeIndex(IntPredicate taken, int currentIndex) {
        for (int candidate = 1;; candidate++) {
            if (candidate == currentIndex) {
                continue;
            }
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }

    /** Trims a submitted name and drops control characters, which would corrupt the nameplate. */
    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder cleaned = new StringBuilder(Math.min(raw.length(), MAX_NAME_LENGTH));
        for (int i = 0; i < raw.length() && cleaned.length() < MAX_NAME_LENGTH; i++) {
            char c = raw.charAt(i);
            if (!Character.isISOControl(c)) {
                cleaned.append(c);
            }
        }
        return cleaned.toString().trim();
    }

    /** Whether a normalized name is something the game is willing to display. */
    public static boolean isValidName(String normalizedName) {
        return normalizedName != null
                && !normalizedName.isEmpty()
                && normalizedName.length() <= MAX_NAME_LENGTH;
    }

    /**
     * Whether two tags collide. Compared case-insensitively, so {@code Reef} and {@code reef} count
     * as the same tag rather than two that merely look alike.
     */
    public static boolean sameName(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    /**
     * The palette stores plain {@code 0xRRGGBB}; ARGB is what {@code GuiGraphics} and the font
     * renderer want. Feeding them a bare RGB value paints with alpha {@code 0x00}, which renders as
     * nothing at all — worth a named function so no call site has to remember it.
     */
    public static int toArgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /** Whether a colour is one the editor offers, so a client cannot paint a tag neon magenta. */
    public static boolean isPaletteColor(int color) {
        for (int candidate : TAG_COLORS) {
            if (candidate == color) {
                return true;
            }
        }
        return false;
    }

    /** Picks a tag colour that is not {@code currentColor}, so a fresh tag is always visible. */
    public static int pickColor(BoundPicker picker, int currentColor) {
        if (TAG_COLORS.length == 1) {
            return TAG_COLORS[0];
        }
        int color;
        do {
            // floorMod so a misbehaving picker cannot index outside the palette.
            color = TAG_COLORS[Math.floorMod(picker.nextInt(TAG_COLORS.length), TAG_COLORS.length)];
        } while (color == currentColor);
        return color;
    }
}
