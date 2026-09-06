package io.github.realmcraft.companion.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public final class FeatureReadersTest {
    @Test public void playerReadsSlotsArmorDurabilityAndLevel() throws Exception {
        PlayerReader p=PlayerReader.parse(SyntheticFeatureData.player());assertEquals(Integer.valueOf(7),p.level);
        assertEquals(3,p.inventory.size());assertEquals(36,p.inventory.get(2).slot);assertEquals(64,p.inventory.get(1).quantity);
        assertEquals(4,p.armor.get(0).slot);assertEquals(Integer.valueOf(100),p.armor.get(0).durability);
        assertEquals(Integer.valueOf(2),p.inventory.get(2).enchantments.get(7));
    }
    @Test public void playerRejectsTruncationAndDuplicateSlots() throws Exception {
        byte[] data=SyntheticFeatureData.player();for(int n=0;n<data.length;n++)rejectPlayer(Arrays.copyOf(data,n));
        byte[] bad=data.clone(); // second item's slot after the first 49-byte record
        int first=138+7+9;ByteBuffer.wrap(bad).putInt(first+49+43,0);rejectPlayer(bad);
        bad=data.clone();ByteBuffer.wrap(bad).putInt(first+32,-1);rejectPlayer(bad);
        bad=data.clone();ByteBuffer.wrap(bad).putInt(first+28,999);rejectPlayer(bad);
    }
    @Test public void ambiguousExperienceDoesNotInventLevel() throws Exception {
        byte[] b=SyntheticFeatureData.player(),extra=Arrays.copyOf(b,b.length+3);extra[b.length]=0;extra[b.length+1]=41;extra[b.length+2]=1;ByteBuffer.wrap(extra).putInt(5,extra.length-9);assertNull(PlayerReader.parse(extra).level);
    }
    private void rejectPlayer(byte[] b) throws Exception {try{PlayerReader.parse(b);fail("Accepted bad player");}catch(IOException expected){}}
    @Test public void terrainMatchesCoordinatesAndTopBlocks() throws Exception {
        ChunkSurface c=ChunkSurface.decode(SyntheticFeatureData.chunk(-16,32,0),"o.-16,32");assertEquals(-16,c.x);assertEquals(32,c.z);
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){assertEquals(40+x/3+z/4,c.heights[z*16+x]);assertEquals(8,c.ids[z*16+x]);}
        assertEquals(1,ChunkSurface.decode(SyntheticFeatureData.chunk(0,0,1),"n.0,0").dimension);
    }
    @Test public void terrainRejectsCorruptionAndMismatchedFilename() throws Exception {
        byte[] b=SyntheticFeatureData.chunk(0,0,0);
        for(int n:new int[]{0,14,17,b.length-1})rejectChunk(Arrays.copyOf(b,n),"o.0,0");
        rejectChunk(b,"o.16,0");rejectChunk(b,"n.0,0");rejectChunk(b,"o.9999999999999,0");
        byte[] bad=b.clone();ByteBuffer.wrap(bad).putInt(19,Integer.MAX_VALUE);rejectChunk(bad,"o.0,0");
        bad=b.clone();Arrays.fill(bad,27,47,(byte)0);rejectChunk(bad,"o.0,0");
    }
    private void rejectChunk(byte[] b,String n)throws Exception {try{ChunkSurface.decode(b,n);fail("Accepted bad chunk");}catch(IOException expected){}}
    @Test public void snapshotVerifiesFilesBeforeAnalysis() throws Exception {
        File root=Files.createTempDirectory("feature-fixture").toFile();SnapshotStore store=new SnapshotStore(root);SnapshotStore.Snapshot s=store.createDemo();
        SnapshotAnalysis a=SnapshotAnalysis.load(s);assertNotNull(a.player);assertEquals(3,a.chunks.size());assertEquals(0,a.skippedChunks);
        File player=new File(s.directory,"world/player_data");byte[] b=Files.readAllBytes(player.toPath());b[20]^=1;Files.write(player.toPath(),b);
        a=SnapshotAnalysis.load(s);assertNull(a.player);assertTrue(a.playerIssue.contains("checksum"));assertEquals(3,a.chunks.size());
        File chunk=new File(s.directory,"world/o.0,0");Files.write(chunk.toPath(),new byte[]{1});a=SnapshotAnalysis.load(s);assertEquals(1,a.skippedChunks);
    }
    @Test public void snapshotRejectsSymlinksAndForgedManifest() throws Exception {
        File root=Files.createTempDirectory("feature-integrity").toFile();SnapshotStore.Snapshot s=new SnapshotStore(root).createDemo();
        File chunk=new File(s.directory,"world/o.0,0");File external=File.createTempFile("feature-outside",".bin");Files.write(external.toPath(),SyntheticFeatureData.chunk(0,0,0));Files.delete(chunk.toPath());Files.createSymbolicLink(chunk.toPath(),external.toPath());
        assertEquals(1,SnapshotAnalysis.load(s).skippedChunks);
        File manifest=new File(s.directory,"manifest.properties");Properties p=new Properties();try(InputStream in=new FileInputStream(manifest)){p.load(in);}p.setProperty("file.0.path","../outside");try(OutputStream out=new FileOutputStream(manifest)){p.store(out,"");}
        try{SnapshotAnalysis.load(s);fail("Unsafe manifest accepted");}catch(IOException expected){}
    }
}
