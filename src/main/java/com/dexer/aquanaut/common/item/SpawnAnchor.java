package com.dexer.aquanaut.common.item;

/**
 * Where the point the player aimed at lands on a spawned entity.
 *
 * <p>
 * The two cases are genuinely different, which is why this is a choice rather than a constant:
 * clicking a block aims at a <em>surface</em>, so the entity should stand on it, while aiming into
 * open water aims at a <em>place</em>, so the entity should sit at the crosshair. Getting the second
 * one wrong makes an aimed spawn appear a whole entity-height above where the player pointed.
 *
 * <p>
 * Deliberately free of Minecraft types so the offset rule can be unit tested without the game
 * runtime.
 */
public enum SpawnAnchor {

    /** The aim point becomes the entity's feet. */
    BOTTOM,

    /** The aim point becomes the middle of the entity. */
    CENTER;

    /**
     * How far below the aim point the entity's origin sits.
     *
     * @param entityHeight the entity's hitbox height, in blocks
     */
    public double originOffset(double entityHeight) {
        return this == CENTER ? entityHeight / 2.0D : 0.0D;
    }
}
