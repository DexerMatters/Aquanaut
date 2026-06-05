package com.dexer.aquanaut.common.entity.serpentine;

@FunctionalInterface
public interface SegmentChainAlgorithm {
    void updateSegments(AbstractSerpentineEntity head, SerpentineSegment[] segments);
}
