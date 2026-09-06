package io.github.realmcraft.companion.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
public final class PlanningTest {
    @Test public void stockCountsReadableStacksAndReportsMissing()throws Exception{
        List<MapPoint> points=ChunkSurface.decode(SyntheticFeatureData.chunkWithPoints(),"o.0,0").points;ChestStock stock=ChestStock.from(points);assertEquals(5,stock.available(3157));assertEquals(7,stock.missing(3157,12));assertEquals(0,stock.missing(3157,2));
        points.stream().filter(p->p.kind.equals("chest")).forEach(p->p.readable=false);stock=ChestStock.from(points);assertEquals(0,stock.available(3157));assertEquals(1,stock.unknown);
    }
    @Test public void chestDetailsExposeQuantityAndPropertyChanges()throws Exception{
        SnapshotStore store=new SnapshotStore(Files.createTempDirectory("planning-test").toFile());SnapshotAnalysis a=SnapshotAnalysis.load(store.createDemo()),b=SnapshotAnalysis.load(store.createDemo());
        MapPoint chest=b.points.stream().filter(p->p.kind.equals("chest")).findFirst().get();chest.items=Arrays.asList(new PlayerReader.Item(1,3157,9,null,new HashMap<>()));SnapshotDiff diff=SnapshotDiff.between(a,b);assertEquals(1,diff.chestChanges.size());assertEquals(Long.valueOf(4),diff.chestChanges.get(0).quantities.get(3157));assertEquals(chest.x,diff.chestChanges.get(0).point.x);
        chest.items=Arrays.asList(new PlayerReader.Item(1,3157,5,90,new HashMap<>()));assertTrue(SnapshotDiff.between(a,b).chestChanges.get(0).propertiesChanged);
    }
}
