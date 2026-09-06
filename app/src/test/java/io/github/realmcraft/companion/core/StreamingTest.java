package io.github.realmcraft.companion.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public final class StreamingTest {
    @Test public void windowMovesToPreviouslyUnloadedTerrainAndRoundtripsZip()throws Exception{
        SnapshotStore store=new SnapshotStore(Files.createTempDirectory("window-test").toFile());SnapshotStore.Snapshot s=store.importZip(new ByteArrayInputStream(SyntheticFeatureData.wideArchive()));SnapshotAnalysis source=SnapshotAnalysis.open(s);assertEquals(300,source.totalChunks);
        SnapshotAnalysis first=source.window(MapOptions.defaults(),0,0,0,96),far=source.window(MapOptions.defaults(),0,4000,0,96);assertTrue(first.chunks.size()<30);assertFalse(first.chunks.stream().anyMatch(c->c.x==4000));assertTrue(far.chunks.stream().anyMatch(c->c.x==4000));
        ByteArrayOutputStream out=new ByteArrayOutputStream();source.exportZip(out);SnapshotStore.Snapshot imported=store.importZip(new ByteArrayInputStream(out.toByteArray()));assertEquals(s.sha256,imported.sha256);
    }
    @Test public void exportStreamsLargeIndividualFiles()throws Exception{
        SnapshotStore store=new SnapshotStore(Files.createTempDirectory("large-export").toFile());SnapshotStore.Snapshot sample=store.createDemo();ByteArrayOutputStream archive=new ByteArrayOutputStream();try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(archive)){zip.putNextEntry(new java.util.zip.ZipEntry("world_data"));zip.write(Files.readAllBytes(new File(sample.directory,"world/world_data").toPath()));zip.closeEntry();zip.putNextEntry(new java.util.zip.ZipEntry("synthetic-large.bin"));byte[] block=new byte[1024*1024];for(int i=0;i<33;i++)zip.write(block);zip.closeEntry();}SnapshotStore.Snapshot imported=store.importZip(new ByteArrayInputStream(archive.toByteArray()));ByteArrayOutputStream exported=new ByteArrayOutputStream();SnapshotAnalysis.open(imported).exportZip(exported);assertEquals(imported.sha256,store.importZip(new ByteArrayInputStream(exported.toByteArray())).sha256);
    }
    @Test public void diskCacheSurvivesMemoryResetAndRejectsCorruption()throws Exception{
        File cache=Files.createTempDirectory("disk-cache").toFile();TerrainDiskCache.configure(cache);TerrainDiskCache.clear();SnapshotStore.Snapshot s=new SnapshotStore(Files.createTempDirectory("disk-source").toFile()).createDemo();SnapshotAnalysis.load(s);SurfaceDecodeCache.clear();int before=TerrainDiskCache.hits;assertEquals(4,SnapshotAnalysis.load(s).points.size());assertTrue(TerrainDiskCache.hits>before);
        for(File f:cache.listFiles())Files.write(f.toPath(),new byte[]{0});SurfaceDecodeCache.clear();assertEquals(4,SnapshotAnalysis.load(s).points.size());TerrainDiskCache.clear();assertEquals(0,cache.listFiles().length);
    }
    @Test public void cancelDuringExtractionLeavesNoPublishedCopy()throws Exception{
        SnapshotStore store=new SnapshotStore(Files.createTempDirectory("cancel-import").toFile());List<String> phases=new ArrayList<>();store.setProgress((phase,bytes)->{phases.add(phase);if(phase.equals("extract"))Thread.currentThread().interrupt();});try{store.importZip(new ByteArrayInputStream(SyntheticFeatureData.wideArchive()));fail();}catch(InterruptedIOException expected){}finally{Thread.interrupted();}assertTrue(phases.contains("archive"));assertTrue(phases.contains("extract"));assertTrue(store.list().isEmpty());
    }
    @Test public void routeAllocatesOnlyNeededItemsAndStaysInDimension()throws Exception{
        List<MapPoint> points=ChunkSurface.decode(SyntheticFeatureData.chunkWithPoints(),"o.0,0").points;CollectionRoute route=CollectionRoute.plan(points,Collections.singletonMap(3157,8L),0,0,0);assertEquals(1,route.stops.size());assertEquals(Long.valueOf(5),route.stops.get(0).take.get(3157));assertEquals(Long.valueOf(3),route.remaining.get(3157));assertTrue(CollectionRoute.plan(points,Collections.singletonMap(3157,8L),1,0,0).stops.isEmpty());
    }
}
