package com.dexer.aquanaut;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the cursor's pushable contract.
 *
 * <p>
 * This one is a source check rather than a behaviour check, deliberately. The entity cannot be
 * instantiated without the game runtime, and the rule it protects is invisible from the outside and
 * extremely easy to reintroduce: marking the cursor solid looks like an obvious improvement and
 * silently breaks pushing completely.
 *
 * <p>
 * Why it breaks: {@code canBeCollidedWith()} makes an entity a collision obstacle, so a player
 * walking into it is stopped flush against its hitbox. {@code AABB.intersects} is strict, so boxes
 * that merely touch do not count as overlapping, and {@code LivingEntity.pushEntities} searches with
 * the player's exact bounding box — so a solid entity is never found and never pushed. Minecarts get
 * this right by not overriding {@code canBeCollidedWith} at all.
 */
final class CursorPushabilityTest {

    private static final Path ENTITY = Path.of(
            "src/main/java/com/dexer/aquanaut/common/entity/CursorEntity.java");

    /** The exact declaration that would make the cursor un-pushable. */
    private static final String FORBIDDEN_OVERRIDE = "public boolean canBeCollidedWith";

    @Test
    void theCursorIsNotACollisionObstacleOrItCannotBePushed() throws IOException {
        String source = Files.readString(ENTITY, StandardCharsets.UTF_8);
        assertFalse(source.contains(FORBIDDEN_OVERRIDE),
                "CursorEntity must not override canBeCollidedWith(): a solid cursor is stopped flush "
                        + "against the player, so LivingEntity.pushEntities never finds it and it can "
                        + "never be pushed. Minecarts leave this unset.");
    }

    @Test
    void theCursorDeclaresTheFlagsThatMakeItPushable() throws IOException {
        String source = Files.readString(ENTITY, StandardCharsets.UTF_8);
        assertTrue(source.contains("public boolean isPushable"),
                "pushEntities only considers entities that report isPushable()");
        assertTrue(source.contains("public boolean isPickable"),
                "isPickable() is what lets the crosshair target the cursor for right-click");
        assertTrue(source.contains("public void push(Entity entity)"),
                "push(Entity) is the hook that actually moves the cursor");
    }
}
