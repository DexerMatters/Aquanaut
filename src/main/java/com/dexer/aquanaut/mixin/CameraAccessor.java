package com.dexer.aquanaut.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The camera's eye height, which is state that belongs to the last entity it was ticked against
 * rather than to the entity it is being set up for.
 *
 * <p>
 * {@code Camera.tick()} walks the eye height half-way toward {@code entity.getEyeHeight()} once per
 * client tick, and {@code Camera.setup} then places the camera at the running average of the two
 * fields. A second render into the same camera — the drone feed's — swaps in a different entity but
 * does not tick, so it inherits the operator's eye height and the drone flies its sortie from a
 * viewpoint more than a block above its own hull. Neither field has a setter, hence this accessor.
 */
@Mixin(Camera.class)
public interface CameraAccessor {

    /** The height the camera has currently settled at. */
    @Accessor("eyeHeight")
    float aquanaut$getEyeHeight();

    /** Sets the settled height, so a second setup into the same camera starts from it. */
    @Accessor("eyeHeight")
    void aquanaut$setEyeHeight(float eyeHeight);

    /** The previous tick's height, the low half of the running average. */
    @Accessor("eyeHeightOld")
    float aquanaut$getEyeHeightOld();

    /** Sets the previous tick's height alongside {@link #aquanaut$setEyeHeight(float)}. */
    @Accessor("eyeHeightOld")
    void aquanaut$setEyeHeightOld(float eyeHeightOld);
}
