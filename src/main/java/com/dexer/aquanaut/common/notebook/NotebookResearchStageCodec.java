package com.dexer.aquanaut.common.notebook;

import com.mojang.serialization.Codec;

public final class NotebookResearchStageCodec {

    public static final Codec<NotebookResearchStage> CODEC = Codec.STRING.xmap(
            NotebookResearchStage::fromSerializedName,
            NotebookResearchStage::serializedName);

    private NotebookResearchStageCodec() {
    }
}
