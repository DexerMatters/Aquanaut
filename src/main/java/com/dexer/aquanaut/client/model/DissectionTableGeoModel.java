package com.dexer.aquanaut.client.model;

import com.dexer.aquanaut.common.block.DissectionTableMultiblock;
import com.dexer.aquanaut.common.block.entity.DissectionTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Picks the merged dissection bench model for a table cell.
 *
 * <p>
 * The renderer resolves the merge group and stores it on the block entity's render state before the
 * model is asked for its resources, so the suffix here always matches the bench actually being
 * drawn. Textures live with the other block atlases under {@code textures/block}.
 */
public class DissectionTableGeoModel extends GeoModel<DissectionTableBlockEntity> {
    private String suffix = "";

    public void useSuffix(String suffix) {
        this.suffix = suffix;
    }

    public void useGroup(DissectionTableMultiblock.Group group) {
        this.suffix = group == null ? "" : group.modelSuffix();
    }

    @Override
    public ResourceLocation getModelResource(DissectionTableBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "geo/dissection_table" + this.suffix + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DissectionTableBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("aquanaut",
                "textures/block/dissection_table" + this.suffix + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(DissectionTableBlockEntity animatable) {
        // Static benches: no animation file is shipped.
        return ResourceLocation.fromNamespaceAndPath("aquanaut", "animations/dissection_table.animation.json");
    }
}
