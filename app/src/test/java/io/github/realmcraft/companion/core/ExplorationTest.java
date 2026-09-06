package io.github.realmcraft.companion.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
public final class ExplorationTest {
    @Test public void exactLayerDoesNotSubstituteGroundBelowAir()throws Exception{
        byte[] data=SyntheticFeatureData.chunkWithPoints();ChunkSurface surface=ChunkSurface.decode(data,"o.0,0",60),slice=ChunkSurface.decode(data,"o.0,0",60,true);
        assertTrue(surface.heights[0]>=0);assertEquals(-1,slice.heights[0]);assertEquals(162,slice.ids[2*16+2]);assertEquals(4,slice.points.size());
    }
    @Test public void comparisonKeepsUnknownCoverageSeparateAndAggregatesItems()throws Exception{
        SnapshotStore store=new SnapshotStore(Files.createTempDirectory("compare-test").toFile());SnapshotAnalysis a=SnapshotAnalysis.load(store.createDemo()),b=SnapshotAnalysis.load(store.createDemo());SnapshotDiff same=SnapshotDiff.between(a,b);assertTrue(same.inventory.isEmpty());assertEquals(0,same.changedColumns);assertEquals(0,same.changedChests);
        b.chunks.get(0).ids[0]=999;SnapshotDiff changed=SnapshotDiff.between(a,b);assertEquals(1,changed.changedColumns);
        b.chunks.remove(0);assertEquals(1,SnapshotDiff.between(a,b).missingChunks);
        b.points.stream().filter(p->p.kind.equals("chest")).forEach(p->p.readable=false);assertTrue(SnapshotDiff.between(a,b).unknownChests>0);
    }
    @Test public void cacheSeparatesSurfaceAndExactLayer()throws Exception{
        SnapshotStore store=new SnapshotStore(Files.createTempDirectory("slice-cache").toFile());SnapshotStore.Snapshot sample=store.createDemo();
        SnapshotAnalysis.load(sample,new MapOptions(100,60,0,0,0));SnapshotAnalysis b=SnapshotAnalysis.load(sample,new MapOptions(100,60,0,0,0,true));assertEquals(-1,b.chunks.get(0).heights[0]);
    }
}
