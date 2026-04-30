package com.dexer.aquanaut.core;

import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;

public final class GameRuleRegistry {

    public static final GameRules.Key<GameRules.IntegerValue> REGEN_FILL_TIME_TICKS =
            GameRules.register("aquanautRegenFillTimeTicks", GameRules.Category.PLAYER,
                    GameRules.IntegerValue.create(50));

    private GameRuleRegistry() {
    }

    public static void register(IEventBus eventBus) {
    }
}
