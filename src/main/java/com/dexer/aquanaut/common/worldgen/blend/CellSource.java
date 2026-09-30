package com.dexer.aquanaut.common.worldgen.blend;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolver of per-field-cell features (volcanic edifices, satellite vents, sedimentary
 * massifs). Geometry classes query cells through a source so a chunk build can memoize
 * the expensive record construction — the nine-cell neighbourhood scan of a 16-block
 * chunk touches at most a handful of distinct cells, while the naive path rebuilds every
 * edifice for every column.
 *
 * <p>Memoization never changes results: sources resolve pure functions of the cell
 * coordinates, so cached and uncached paths are bit-identical and chunk-seamless.</p>
 */
public interface CellSource<T> {
    /** The feature seeded for one cell, or {@code null} when the cell stays quiet. */
    T at(int cellX, int cellZ);

    /** HashMap-backed memoizing wrapper; caches negative ({@code null}) results too. */
    static <T> CellSource<T> memoized(CellSource<T> base) {
        return new CellSource<T>() {
            private final Map<Long, T> cache = new HashMap<>();

            @Override
            public T at(int cellX, int cellZ) {
                long key = (((long) cellX) << 32) ^ (cellZ & 0xFFFFFFFFL);
                if (cache.containsKey(key)) {
                    return cache.get(key);
                }
                T value = base.at(cellX, cellZ);
                cache.put(key, value);
                return value;
            }
        };
    }
}
