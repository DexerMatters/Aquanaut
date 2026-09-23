package com.dexer.aquanaut.client.renderer;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * What a return means, and the colour it is plotted in.
 *
 * <p>
 * Three answers, and the order they are asked in is the whole of the rule: something that is
 * <em>built</em> to fight the player is red before anything else is considered, because a piglin is
 * also a neutral mob and a zombified piglin answers to both; something that fights back only when it
 * is provoked, or another player, whose intent nothing on this side of the screen can know, is
 * yellow; everything else that is alive is white. Armour stands are filtered out before this is even
 * asked — a detector reports creatures, and a rack of armour is furniture.
 */
enum DetectorSignal {

    /** Red: mobs that exist to attack. */
    HOSTILE(1.00F, 0.25F, 0.19F),

    /** Yellow: mobs that only turn on you, and other players. */
    NEUTRAL(1.00F, 0.80F, 0.22F),

    /** White: everything else that is alive. */
    PASSIVE(1.00F, 1.00F, 1.00F);

    final float red;
    final float green;
    final float blue;

    DetectorSignal(float red, float green, float blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    static DetectorSignal of(LivingEntity entity) {
        if (entity instanceof Enemy) {
            return HOSTILE;
        }
        if (entity instanceof NeutralMob || entity instanceof Player) {
            return NEUTRAL;
        }
        return PASSIVE;
    }
}
