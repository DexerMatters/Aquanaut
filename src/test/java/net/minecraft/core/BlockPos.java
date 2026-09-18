package net.minecraft.core;

import java.util.Objects;

/**
 * Test-side shim for {@code net.minecraft.core.BlockPos}.
 *
 * <p>
 * The JUnit tests run without the Minecraft runtime, so the mod classes they exercise are linked
 * against the small shims under {@code src/test/java} instead. Only the surface used by the tested
 * classes is provided; behaviour must match vanilla (immutable, value equality).
 */
public final class BlockPos implements Comparable<BlockPos> {

    private final int x;
    private final int y;
    private final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(this.x + dx, this.y + dy, this.z + dz);
    }

    public BlockPos west() {
        return new BlockPos(this.x - 1, this.y, this.z);
    }

    public BlockPos east() {
        return new BlockPos(this.x + 1, this.y, this.z);
    }

    public BlockPos north() {
        return new BlockPos(this.x, this.y, this.z - 1);
    }

    public BlockPos south() {
        return new BlockPos(this.x, this.y, this.z + 1);
    }

    public BlockPos below() {
        return new BlockPos(this.x, this.y - 1, this.z);
    }

    public BlockPos above() {
        return new BlockPos(this.x, this.y + 1, this.z);
    }

    @Override
    public int compareTo(BlockPos other) {
        int result = Integer.compare(this.y, other.y);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(this.z, other.z);
        if (result != 0) {
            return result;
        }
        return Integer.compare(this.x, other.x);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlockPos pos)) {
            return false;
        }
        return this.x == pos.x && this.y == pos.y && this.z == pos.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return "BlockPos(" + this.x + ", " + this.y + ", " + this.z + ")";
    }
}
