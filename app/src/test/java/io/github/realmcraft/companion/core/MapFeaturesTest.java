package io.github.realmcraft.companion.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class MapFeaturesTest {
    @Test public void deletionRemovesOnlySelectedCopyAndNeverLinkTarget()throws Exception{
        File root=Files.createTempDirectory("delete-test").toFile();SnapshotStore store=new SnapshotStore(root);SnapshotStore.Snapshot a=store.createDemo(),b=store.createDemo();
        File external=File.createTempFile("keep-external",".txt");Files.write(external.toPath(),new byte[]{7});
        Files.createSymbolicLink(new File(a.directory,"world/external").toPath(),external.toPath());store.delete(a.id);
        assertFalse(a.directory.exists());assertArrayEquals(new byte[]{7},Files.readAllBytes(external.toPath()));assertEquals(1,store.list().size());assertEquals(b.id,store.list().get(0).id);
        for(String bad:new String[]{"..",b.id+"/..","../"+b.id})try{store.delete(bad);fail();}catch(IOException expected){}
        assertNotNull(SnapshotAnalysis.load(b).player);
    }
    @Test public void pointsAreBlockAnchoredAndContentsAreDecoded()throws Exception{
        ChunkSurface c=ChunkSurface.decode(SyntheticFeatureData.chunkWithPoints(),"o.0,0");assertEquals(4,c.points.size());
        MapPoint sign=point(c,"sign"),chest=point(c,"chest");assertEquals("Workshop → storage",sign.text);assertTrue(sign.readable);
        assertTrue(chest.readable);assertEquals(1,chest.items.size());assertEquals(5,chest.items.get(0).quantity);assertEquals(3157,chest.items.get(0).id);
        assertEquals(60,point(c,"bed").y);assertEquals(158,point(c,"crafting").blockId);
    }
    @Test public void duplicateSignAndChestRecordsBecomeUnknown()throws Exception{
        byte[] data=SyntheticFeatureData.chunkWithPoints();int marker=find(data,new byte[]{0,(byte)162,0,0,0},15);
        byte[] duplicate=Arrays.copyOf(data,data.length+data.length-marker);System.arraycopy(data,marker,duplicate,data.length,data.length-marker);
        ChunkSurface c=ChunkSurface.decode(duplicate,"o.0,0");assertFalse(point(c,"sign").readable);assertEquals("",point(c,"sign").text);assertFalse(point(c,"chest").readable);assertTrue(point(c,"chest").items.isEmpty());
    }
    @Test public void missingRecordsDoNotPretendToBeEmpty()throws Exception{
        byte[] data=SyntheticFeatureData.chunkWithPoints();int marker=find(data,new byte[]{0,(byte)162,0,0,0},15);
        ChunkSurface c=ChunkSurface.decode(Arrays.copyOf(data,marker),"o.0,0");assertFalse(point(c,"sign").readable);assertFalse(point(c,"chest").readable);
    }
    @Test public void renderHeightAndAreaAndBudgetsAreHonored()throws Exception{
        ChunkSurface c=ChunkSurface.decode(SyntheticFeatureData.chunkWithPoints(),"o.0,0",30);assertEquals(30,c.heights[34]);assertEquals(1,c.ids[34]);assertEquals(4,c.points.size());
        File root=Files.createTempDirectory("render-test").toFile();SnapshotStore.Snapshot s=new SnapshotStore(root).createDemo();
        SnapshotAnalysis a=SnapshotAnalysis.load(s,new MapOptions(1,30,0,0,0));assertEquals(1,a.chunks.size());assertTrue(a.limited);
        a=SnapshotAnalysis.load(s,new MapOptions(100,255,64,10000,10000));assertTrue(a.chunks.isEmpty());assertNotNull(a.player);
        MapOptions m=new MapOptions(Integer.MAX_VALUE,-1,128,Integer.MAX_VALUE,Integer.MIN_VALUE);assertEquals(16384,m.chunks);assertEquals(0,m.ceiling);assertFalse(m.includes(0,0));
    }
    @Test public void truncatedSignTextAndBadChestCountsAreRejected()throws Exception{
        byte[] data=SyntheticFeatureData.chunkWithPoints();int sign=find(data,new byte[]{0,(byte)162,0,0,0},15),chest=find(data,new byte[]{0,(byte)153,0,0,0},sign);
        byte[] b=data.clone();java.nio.ByteBuffer.wrap(b).putInt(sign+21,Integer.MAX_VALUE);assertFalse(point(ChunkSurface.decode(b,"o.0,0"),"sign").readable);
        b=data.clone();java.nio.ByteBuffer.wrap(b).putInt(chest+24,28);assertFalse(point(ChunkSurface.decode(b,"o.0,0"),"chest").readable);
    }
    private MapPoint point(ChunkSurface c,String kind){return c.points.stream().filter(p->p.kind.equals(kind)).findFirst().get();}
    private int find(byte[] b,byte[] needle,int start){outer:for(int i=start;i<=b.length-needle.length;i++){for(int j=0;j<needle.length;j++)if(b[i+j]!=needle[j])continue outer;return i;}throw new AssertionError("No marker");}
}
