package com.dexer.aquanaut.common.inventory.aquarium;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class AquariumCommandEvents {

    private AquariumCommandEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("givefish")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("fish", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        AquariumFishCatalog.commandSuggestions(), builder))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                    ResourceLocation fishId = ResourceLocationArgument.getId(context, "fish");
                                    AquariumFishSpec spec = AquariumFishCatalog.byId(fishId).orElse(null);
                                    if (spec == null) {
                                        context.getSource().sendFailure(
                                                Component.translatable("command.aquanaut.givefish.unknown_fish",
                                                        fishId.toString()));
                                        return 0;
                                    }

                                    boolean added = AquariumInventoryHelper.addFish(target, spec);
                                    if (!added) {
                                        context.getSource().sendFailure(
                                                Component.translatable("command.aquanaut.givefish.no_room",
                                                        target.getScoreboardName(),
                                                        spec.entityType().getDescription()));
                                        return 0;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.translatable("command.aquanaut.givefish.success",
                                                    spec.entityType().getDescription(),
                                                    target.getScoreboardName()), false);
                                    return 1;
                                }))));
    }
}
