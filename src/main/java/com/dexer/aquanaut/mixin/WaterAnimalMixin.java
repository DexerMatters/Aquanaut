package com.dexer.aquanaut.mixin;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumFish;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.WaterAnimal;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WaterAnimal.class)
public abstract class WaterAnimalMixin implements AquariumFish {

    @Override
    public float getAquariumModelLength() {
        return ((Entity) (Object) this).getBbWidth();
    }

    @Override
    public float getAquariumModelWidth() {
        return ((Entity) (Object) this).getBbWidth();
    }

    @Override
    public float getAquariumModelHeight() {
        return ((Entity) (Object) this).getBbHeight();
    }
}
