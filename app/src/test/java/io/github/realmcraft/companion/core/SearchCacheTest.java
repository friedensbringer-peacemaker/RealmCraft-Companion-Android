package io.github.realmcraft.companion.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;
public final class SearchCacheTest {
    private List<MapPoint> points()throws Exception{return ChunkSurface.decode(SyntheticFeatureData.chunkWithPoints(),"o.0,0").points;}
    @Test public void searchFindsSignsAndAggregatesMatchingStacks()throws Exception{
        List<MapPoint> p=points();assertEquals(1,PointSearch.find(p,"WORKSHOP",false,id->"Block").size());
        MapPoint chest=p.stream().filter(v->v.kind.equals("chest")).findFirst().get();
        chest.items=Arrays.asList(new PlayerReader.Item(1,3157,5,null,new HashMap<>()),new PlayerReader.Item(2,3157,12,null,new HashMap<>()));
        assertEquals(17,PointSearch.find(p,"diamant",true,id->"Diamant").get(0).quantity);
        assertEquals(17,PointSearch.find(p,"3157",true,id->"Diamant").get(0).quantity);
        assertTrue(PointSearch.find(p,"missing",true,id->"Diamant").isEmpty());
        chest.readable=false;assertTrue(PointSearch.find(p,"diamant",true,id->"Diamant").isEmpty());
    }
    @Test public void searchHandlesAccentsEmptyQueriesAndDimensions()throws Exception{
        List<MapPoint> p=points();assertEquals(4,PointSearch.find(p,"werkbank",false,id->"Wérkbank").size());
        assertTrue(PointSearch.find(p,"  ",false,id->"Block").isEmpty());
        MapPoint nether=new MapPoint(0,1,0,1,162,"sign");nether.readable=true;nether.text="Workshop";p.add(nether);
        assertEquals(2,PointSearch.find(p,"workshop",false,id->"Block").size());
    }
    @Test public void cachePreservesPointsAndSeparatesHeightAndVerifiesChangedFiles()throws Exception{
        SurfaceDecodeCache.clear();SnapshotStore.Snapshot sample=new SnapshotStore(Files.createTempDirectory("search-cache").toFile()).createDemo();
        SnapshotAnalysis first=SnapshotAnalysis.load(sample);int count=first.points.size();SnapshotAnalysis second=SnapshotAnalysis.load(sample);
        assertTrue(SurfaceDecodeCache.hits>0);assertEquals(count,second.points.size());
        assertEquals(30,SnapshotAnalysis.load(sample,new MapOptions(100,30,0,0,0)).chunks.get(0).heights[0]);
        Path chunk=Files.walk(new File(sample.directory,"world").toPath()).filter(p->p.getFileName().toString().equals("o.0,0")).findFirst().get();
        byte[] bytes=Files.readAllBytes(chunk);bytes[0]^=1;Files.write(chunk,bytes);
        assertTrue(SnapshotAnalysis.load(sample).skippedChunks>0);
    }
}
