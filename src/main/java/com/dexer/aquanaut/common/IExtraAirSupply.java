package com.dexer.aquanaut.common;

/**
 * Mixin interface injected onto
 * {@link net.minecraft.world.entity.player.Player}.
 * Cast any player instance to this interface to read/write the extra air supply
 * state.
 *
 * <pre>{@code
 * IExtraAirSupply supply = (IExtraAirSupply) player;
 * supply.aquanaut$setMaxExtraAirSupply(300);
 * supply.aquanaut$setExtraAirSupply(300);
 * }</pre>
 */
public interface IExtraAirSupply {
    int aquanaut$getExtraAirSupply();

    void aquanaut$setExtraAirSupply(int value);

    int aquanaut$getMaxExtraAirSupply();

    void aquanaut$setMaxExtraAirSupply(int value);
}
