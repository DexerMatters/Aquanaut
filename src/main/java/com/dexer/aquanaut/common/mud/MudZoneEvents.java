package com.dexer.aquanaut.common.mud;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.worldgen.GlowMushroomBuilder;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;

/** Bone-mealing a glow fungus grows it into a huge glow mushroom, like a small mushroom. */
@EventBusSubscriber(modid = Aquanaut.MODID)
public final class MudZoneEvents {
    private MudZoneEvents() {
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        BlockState state = event.getState();
        if (!(state.is(BlockRegistry.GLOW_FUNGUS.get())
                || state.is(BlockRegistry.GLOW_FUNGUS_AMBER.get())
                || state.is(BlockRegistry.GLOW_FUNGUS_VIOLET.get()))) {
            return;
        }
        if (GlowMushroomBuilder.grow(event.getLevel(), event.getPos(), event.getLevel().getRandom())) {
            event.setCanceled(true);
        }
    }
}
