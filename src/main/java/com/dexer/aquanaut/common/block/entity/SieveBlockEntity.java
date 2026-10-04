package com.dexer.aquanaut.common.block.entity;

import com.dexer.aquanaut.core.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Block entity that carries the sifting sieve's geometry and its shaking loop.
 *
 * <p>
 * The sieve has no sifting logic yet, so the animation is purely decorative and always plays: the
 * stone tray slides left and right over the net bag with a small bounce, exactly as the Blockbench
 * authoring animation does. When filtering is implemented, this is the single place that has to
 * become conditional (play while working, hold still otherwise).
 */
public class SieveBlockEntity extends BlockEntity implements GeoBlockEntity {
    /** The authored loop: the {@code sift} animation in {@code animations/sieve.animation.json}. */
    private static final RawAnimation SIFT = RawAnimation.begin().thenLoop("animation.sieve.sift");

    public SieveBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SIEVE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "sift", 0, state -> state.setAndContinue(SIFT)));
    }

    private AnimatableInstanceCache cache;

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        AnimatableInstanceCache current = this.cache;
        if (current == null) {
            current = this.cache = GeckoLibUtil.createInstanceCache(this);
        }
        return current;
    }
}
