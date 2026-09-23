package com.dexer.aquanaut;

import com.dexer.aquanaut.common.entity.TagRules;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cursor tag rules: {@code #N} numbering, player-typed names and the colour palette.
 *
 * <p>
 * Duplicate tags are the thing that must never happen, so the uniqueness rule is tested as a pure
 * function rather than through the entity and its editor.
 */
final class TagRulesTest {

    // ------------------------------------------------------------------
    // automatic numbering
    // ------------------------------------------------------------------

    @Test
    void firstIndexOnAnEmptyWorldIsOne() {
        assertEquals(1, TagRules.firstFreeIndex(index -> false, 0));
    }

    @Test
    void numberingSkipsTheNumbersOtherCursorsHold() {
        Set<Integer> taken = Set.of(1, 2, 4);
        assertEquals(3, TagRules.firstFreeIndex(taken::contains, 0));
    }

    @Test
    void allocatingTwoHundredTagsNeverRepeatsOne() {
        Set<Integer> taken = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            int index = TagRules.firstFreeIndex(taken::contains, 0);
            assertFalse(taken.contains(index), "tag #" + index + " was handed out twice");
            taken.add(index);
        }
        assertEquals(200, taken.size());
    }

    @Test
    void theAutoLabelIsHashPrefixed() {
        assertEquals("#1", TagRules.format(1));
        assertEquals("#42", TagRules.format(42));
    }

    @Test
    void firstFreeNameSkipsTheNamesAlreadyWorn() {
        // #1 and #2 are taken, and a cursor has been renamed to something else entirely.
        Set<String> worn = Set.of("#1", "#2", "Reef");
        assertEquals("#3", TagRules.firstFreeName(worn::contains));
    }

    @Test
    void aCustomNameDoesNotBlockTheNumberItMerelyResembles() {
        Set<String> worn = Set.of("1", "#1x");
        assertEquals("#1", TagRules.firstFreeName(worn::contains));
    }

    // ------------------------------------------------------------------
    // names the player types
    // ------------------------------------------------------------------

    @Test
    void namesAreTrimmedAndStrippedOfControlCharacters() {
        assertEquals("Reef", TagRules.normalizeName("  Reef  "));
        assertEquals("Reef", TagRules.normalizeName("Re\nef"));
        assertEquals("Reef", TagRules.normalizeName("Re\u0000ef"));
    }

    @Test
    void namesAreCappedAtTheDisplayLimit() {
        String tooLong = "x".repeat(TagRules.MAX_NAME_LENGTH + 20);
        String normalized = TagRules.normalizeName(tooLong);
        assertEquals(TagRules.MAX_NAME_LENGTH, normalized.length());
        assertTrue(TagRules.isValidName(normalized));
    }

    @Test
    void blankNamesAreRejected() {
        assertFalse(TagRules.isValidName(TagRules.normalizeName(null)));
        assertFalse(TagRules.isValidName(TagRules.normalizeName("")));
        assertFalse(TagRules.isValidName(TagRules.normalizeName("   ")));
        assertFalse(TagRules.isValidName(TagRules.normalizeName("\n\t")));
    }

    @Test
    void anUnnormalizedNameOverTheLimitIsRejected() {
        assertFalse(TagRules.isValidName("x".repeat(TagRules.MAX_NAME_LENGTH + 1)));
    }

    @Test
    void duplicateDetectionIgnoresCase() {
        // Otherwise "Reef" and "reef" would be two tags nobody can tell apart.
        assertTrue(TagRules.sameName("Reef", "reef"));
        assertTrue(TagRules.sameName("#3", "#3"));
        assertFalse(TagRules.sameName("Reef", "Reefs"));
        assertFalse(TagRules.sameName(null, "Reef"));
        assertFalse(TagRules.sameName("Reef", null));
    }

    // ------------------------------------------------------------------
    // colour
    // ------------------------------------------------------------------

    @Test
    void aFreshTagNeverReusesTheColourItAlreadyHad() {
        Random random = new Random(1234L);
        int current = TagRules.TAG_COLORS[0];
        for (int i = 0; i < 500; i++) {
            int next = TagRules.pickColor(random::nextInt, current);
            assertNotEquals(current, next, "a reroll the player cannot see is a bug");
            current = next;
        }
    }

    @Test
    void pickingAlwaysLandsInThePalette() {
        Random random = new Random(99L);
        for (int i = 0; i < 200; i++) {
            int picked = TagRules.pickColor(random::nextInt, -1);
            assertTrue(TagRules.isPaletteColor(picked),
                    "picked " + Integer.toHexString(picked) + " is not a tag colour");
        }
    }

    @Test
    void aMisbehavingRandomSourceCannotEscapeThePalette() {
        // Defensive: the server validates whatever it is handed, but a broken picker should not be
        // able to produce an out-of-palette colour in the first place.
        for (int outOfRange : new int[] { -5, -1, 999 }) {
            int picked = TagRules.pickColor(bound -> outOfRange, -1);
            assertTrue(TagRules.isPaletteColor(picked));
        }
    }

    @Test
    void theServerRejectsColoursTheEditorDoesNotOffer() {
        assertTrue(TagRules.isPaletteColor(TagRules.TAG_COLORS[0]));
        assertFalse(TagRules.isPaletteColor(0xFF00FF));
        assertFalse(TagRules.isPaletteColor(0));
    }

    @Test
    void aRenderableTagColourIsAlwaysFullyOpaque() {
        // The palette stores 0xRRGGBB and every renderer needs ARGB. Handing a bare RGB value to
        // GuiGraphics paints with alpha 0x00 — which is invisible, and is exactly how the editor
        // first shipped with "colours" that were all black.
        for (int rgb : TagRules.TAG_COLORS) {
            int argb = TagRules.toArgb(rgb);
            assertEquals(0xFF, argb >>> 24,
                    "tag colour " + Integer.toHexString(rgb) + " is not opaque once rendered");
            assertEquals(rgb, argb & 0xFFFFFF,
                    "converting to ARGB must not disturb the colour itself");
        }
    }

    @Test
    void everyTagColourIsDistinctAndOpaque() {
        Set<Integer> seen = new HashSet<>();
        for (int color : TagRules.TAG_COLORS) {
            assertTrue(seen.add(color), "duplicate tag colour " + Integer.toHexString(color));
            assertEquals(0, color & 0xFF000000,
                    "tag colours are 0xRRGGBB, got " + Integer.toHexString(color));
        }
    }
}
