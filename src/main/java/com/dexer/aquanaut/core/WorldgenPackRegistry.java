package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Ships the abyssal overworld height as a discoverable data pack instead of a hidden
 * {@code data/minecraft/...} override.
 *
 * <p>
 * The pack deepens {@code minecraft:overworld} (and the {@code overworld} / {@code large_biomes} /
 * {@code amplified} noise settings that its presets use) to {@code min_y = -512}, {@code height =
 * 832}. Because it is registered with {@link PackSource#DEFAULT} and {@code alwaysActive = false},
 * it appears in the Data Packs screen enabled by default and can be switched off — an override this
 * invasive should be visible and reversible rather than silent. Disabling it <em>after</em> a world
 * has generated terrain below Y=-64 deletes that range on the next save, which is why the pack
 * description says so.
 * </p>
 *
 * <p>
 * Aquanaut's own worlds do not depend on this pack: the water world preset points at
 * {@code aquanaut:abyssal_overworld}, which the mod always ships.
 * </p>
 */
public final class WorldgenPackRegistry {

    /** Pack root inside the mod jar, resolved from {@code resources/abyssal_overworld_pack}. */
    public static final ResourceLocation ABYSSAL_OVERWORLD_PACK =
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "abyssal_overworld_pack");

    private WorldgenPackRegistry() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        event.addPackFinders(ABYSSAL_OVERWORLD_PACK, PackType.SERVER_DATA,
                Component.translatable("pack.aquanaut.abyssal_overworld"),
                PackSource.DEFAULT, false, Pack.Position.TOP);
    }
}
