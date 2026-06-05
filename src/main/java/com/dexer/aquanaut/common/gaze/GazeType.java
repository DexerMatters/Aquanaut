package com.dexer.aquanaut.common.gaze;

import com.mojang.serialization.Codec;

public enum GazeType {
    FAVOR,
    DEVOUR;

    public static final Codec<GazeType> CODEC = Codec.STRING.xmap(GazeType::fromSerializedName, GazeType::serializedName);

    public String serializedName() {
        return switch (this) {
            case FAVOR -> "favor";
            case DEVOUR -> "devour";
        };
    }

    private static GazeType fromSerializedName(String name) {
        return switch (name) {
            case "favor" -> FAVOR;
            case "devour" -> DEVOUR;
            default -> throw new IllegalArgumentException("Unknown gaze type: " + name);
        };
    }
}
