package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.IExtraAirSupply;
import com.dexer.aquanaut.core.AttachmentRegistry;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Player.class)
public abstract class PlayerMixin implements IExtraAirSupply {

    @Unique
    @Override
    public int aquanaut$getExtraAirSupply() {
        return ((Player) (Object) this).getData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get());
    }

    @Unique
    @Override
    public void aquanaut$setExtraAirSupply(int value) {
        ((Player) (Object) this).setData(AttachmentRegistry.EXTRA_AIR_SUPPLY.get(), value);
    }

    @Unique
    @Override
    public int aquanaut$getMaxExtraAirSupply() {
        return ((Player) (Object) this).getData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get());
    }

    @Unique
    @Override
    public void aquanaut$setMaxExtraAirSupply(int value) {
        ((Player) (Object) this).setData(AttachmentRegistry.MAX_EXTRA_AIR_SUPPLY.get(), value);
    }
}
