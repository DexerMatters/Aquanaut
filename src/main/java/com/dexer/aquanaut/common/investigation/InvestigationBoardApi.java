package com.dexer.aquanaut.common.investigation;

import com.dexer.aquanaut.network.InvestigationProgressSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Public server API for gameplay systems that reveal evidence or advance a node.
 * All mutations target the overworld SavedData, making progress shared by every board and player.
 */
public final class InvestigationBoardApi {
    private InvestigationBoardApi() {
    }

    public static InvestigationProgress getProgress(MinecraftServer server) {
        return data(server).snapshot();
    }

    public static boolean setNodeStars(MinecraftServer server, ResourceLocation nodeId, int stars) {
        InvestigationNode node = InvestigationCatalog.node(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("unknown investigation node: " + nodeId));
        boolean changed = data(server).setStars(node, stars);
        if (changed) syncToAll(server);
        return changed;
    }

    public static boolean advanceNode(MinecraftServer server, ResourceLocation nodeId) {
        InvestigationNode node = InvestigationCatalog.node(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("unknown investigation node: " + nodeId));
        InvestigationProgress progress = getProgress(server);
        return setNodeStars(server, nodeId, progress.stars(node) + 1);
    }

    public static boolean discoverLink(MinecraftServer server, ResourceLocation linkId) {
        InvestigationLink link = InvestigationCatalog.link(linkId)
                .orElseThrow(() -> new IllegalArgumentException("unknown investigation link: " + linkId));
        boolean changed = data(server).discover(link);
        if (changed) syncToAll(server);
        return changed;
    }

    public static void syncTo(ServerPlayer player, boolean openBoard) {
        PacketDistributor.sendToPlayer(player,
                InvestigationProgressSyncPayload.fromData(getProgress(player.getServer()), openBoard));
    }

    public static void open(ServerPlayer player) {
        syncTo(player, true);
    }

    private static InvestigationSavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(InvestigationSavedData.FACTORY, InvestigationSavedData.FILE_ID);
    }

    private static void syncToAll(MinecraftServer server) {
        InvestigationProgressSyncPayload payload = InvestigationProgressSyncPayload.fromData(getProgress(server), false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
