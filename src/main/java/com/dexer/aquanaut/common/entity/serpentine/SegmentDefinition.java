package com.dexer.aquanaut.common.entity.serpentine;

public final class SegmentDefinition {
    public final float width;
    public final float height;
    public final float spacing;
    public final boolean canBeHit;

    private SegmentDefinition(float width, float height, float spacing, boolean canBeHit) {
        if (width <= 0)
            throw new IllegalArgumentException("SegmentDefinition width must be > 0");
        if (height <= 0)
            throw new IllegalArgumentException("SegmentDefinition height must be > 0");
        if (spacing <= 0)
            throw new IllegalArgumentException("SegmentDefinition spacing must be > 0");
        this.width = width;
        this.height = height;
        this.spacing = spacing;
        this.canBeHit = canBeHit;
    }

    public static SegmentDefinition of(float width, float height, float spacing) {
        return new SegmentDefinition(width, height, spacing, true);
    }

    public static Builder builder(float width, float height, float spacing) {
        return new Builder(width, height, spacing);
    }

    public static final class Builder {
        private final float width;
        private final float height;
        private final float spacing;
        private boolean canBeHit = true;

        private Builder(float width, float height, float spacing) {
            this.width = width;
            this.height = height;
            this.spacing = spacing;
        }

        public Builder canBeHit(boolean canBeHit) {
            this.canBeHit = canBeHit;
            return this;
        }

        public SegmentDefinition build() {
            return new SegmentDefinition(width, height, spacing, canBeHit);
        }
    }
}
