package io.github.realmcraft.companion.core;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

public class SnapshotStoreTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private SnapshotStore store;
    @Before public void setup() throws Exception { store = new SnapshotStore(temp.newFolder("library")); }
    private byte[] metadata() {
        byte[] name = "Synthetic test world".getBytes(StandardCharsets.UTF_8);
        ByteBuffer b = ByteBuffer.allocate(17 + name.length + 105);
        b.put((byte) 9).putInt(42); b.position(9); b.putInt(12345).putInt(name.length).put(name); return b.array();
    }
    private byte[] archive(String[] paths, byte[][] data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) { for (int i = 0; i < paths.length; i++) { zip.putNextEntry(new ZipEntry(paths[i])); zip.write(data[i]); zip.closeEntry(); } } return out.toByteArray();
    }
    private byte[] archive(String path, byte[] data) throws Exception { return archive(new String[]{path}, new byte[][]{data}); }
    private void rejects(byte[] zip) throws Exception {
        try { store.importZip(new ByteArrayInputStream(zip)); fail("Expected rejection"); } catch (IOException expected) { assertNotNull(expected.getMessage()); }
        assertTrue(store.list().isEmpty());
    }
    @Test public void nestedWorldPersistsAndRepeatedImportsAreIndependent() throws Exception {
        byte[] zip = archive(new String[]{"export/42/world_data", "export/42/o.0,0"}, new byte[][]{metadata(), new byte[]{1,2,3}});
        SnapshotStore.Snapshot a = store.importZip(new ByteArrayInputStream(zip));
        SnapshotStore.Snapshot b = store.importZip(new ByteArrayInputStream(zip));
        assertNotEquals(a.id, b.id); assertEquals(a.sha256, b.sha256); assertEquals("42", a.worldId); assertEquals("12345", a.seed);
        assertEquals("Synthetic test world", a.name); assertEquals(2, a.fileCount); assertFalse(a.synthetic);
        assertTrue(new File(a.directory, "world/export/42/o.0,0").isFile());
        List<SnapshotStore.Snapshot> saved = new SnapshotStore(a.directory.getParentFile()).list();
        assertEquals(2, saved.size()); assertEquals(a.sha256, saved.get(0).sha256); assertEquals(a.files, saved.get(0).files);
    }
    @Test public void syntheticDemoIsMarkedAndNotPlayable() throws Exception {
        SnapshotStore.Snapshot demo = store.createDemo(); assertTrue(demo.synthetic); assertEquals("42", demo.worldId);
        assertTrue(demo.files.contains("SYNTHETIC-NOT-PLAYABLE.txt")); assertEquals(6, demo.fileCount);
    }
    @Test public void rejectsUnsafePaths() throws Exception {
        for (String path : new String[]{"../world_data", "/world_data", "x/../../world_data", "x\\world_data", "./world_data", "x//world_data", "C:/world_data"}) rejects(archive(path, metadata()));
    }
    @Test public void rejectsMultipleWorlds() throws Exception { rejects(archive(new String[]{"a/world_data", "b/world_data"}, new byte[][]{metadata(), metadata()})); }
    @Test public void rejectsMissingAndWrongMetadata() throws Exception {
        rejects(archive("other", metadata())); rejects(archive("world_data", new byte[17]));
        byte[] bad = metadata(); bad[0] = 8; rejects(archive("world_data", bad));
        bad = metadata(); ByteBuffer.wrap(bad).putInt(13, Integer.MAX_VALUE); rejects(archive("world_data", bad));
        bad = metadata(); bad[17] = (byte) 0xff; rejects(archive("world_data", bad));
    }
    @Test public void rejectsTruncatedArchiveEvenAfterFullEntry() throws Exception {
        byte[] zip = archive("world_data", metadata()); rejects(Arrays.copyOf(zip, zip.length - 22));
    }
    @Test public void rejectsDuplicateCentralDirectoryNames() throws Exception {
        byte[] zip = archive(new String[]{"world_data", "other_data"}, new byte[][]{metadata(), metadata()});
        byte[] from = "other_data".getBytes(StandardCharsets.US_ASCII), to = "world_data".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i <= zip.length - from.length; i++) { boolean match = true; for (int j = 0; j < from.length; j++) if (zip[i+j] != from[j]) match = false; if (match) System.arraycopy(to, 0, zip, i, to.length); }
        rejects(zip);
    }
    @Test public void actualInflatedSizeAndEntryLimitsApply() throws Exception {
        store = new SnapshotStore(temp.newFolder("small"), 1024, 10);
        rejects(archive(new String[]{"world_data", "large"}, new byte[][]{metadata(), new byte[2000]}));
        store = new SnapshotStore(temp.newFolder("few"), 10000, 1);
        rejects(archive(new String[]{"world_data", "extra"}, new byte[][]{metadata(), new byte[]{1}}));
    }
    @Test public void sourceReadAndCloseErrorsPreventCommit() throws Exception {
        final byte[] zip = archive("world_data", metadata());
        InputStream broken = new ByteArrayInputStream(zip) { @Override public void close() throws IOException { throw new IOException("Synthetic pipe failure"); } };
        try { store.importZip(broken); fail(); } catch (IOException expected) { assertEquals("Synthetic pipe failure", expected.getMessage()); }
        assertTrue(store.list().isEmpty());
        broken = new InputStream() { public int read() throws IOException { throw new IOException("Synthetic read failure"); } };
        try { store.importZip(broken); fail(); } catch (IOException expected) { assertEquals("Synthetic read failure", expected.getMessage()); }
        assertTrue(store.list().isEmpty());
    }
    @Test public void nullInputIsRejectedClearly() throws Exception {
        try { store.importZip(null); fail(); } catch (IOException expected) { assertEquals("No archive input provided", expected.getMessage()); }
        assertTrue(store.list().isEmpty());
    }
    @Test public void interruptionBeforeImportClosesInputWithoutPublishing() throws Exception {
        final boolean[] closed = {false};
        InputStream input = new ByteArrayInputStream(archive("world_data", metadata())) {
            @Override public void close() { closed[0] = true; }
        };
        Thread.currentThread().interrupt();
        try { store.importZip(input); fail(); } catch (InterruptedIOException expected) {
            assertTrue("Cancellation must preserve the interrupt flag", Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
        assertTrue(closed[0]); assertTrue(store.list().isEmpty());
    }
    @Test public void interruptionDuringCopyClosesInputWithoutPublishing() throws Exception {
        final boolean[] closed = {false};
        InputStream input = new ByteArrayInputStream(archive("world_data", metadata())) {
            @Override public synchronized int read(byte[] b, int off, int len) {
                int result = super.read(b, off, len); Thread.currentThread().interrupt(); return result;
            }
            @Override public void close() { closed[0] = true; }
        };
        try { store.importZip(input); fail(); } catch (InterruptedIOException expected) {
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
        assertTrue(closed[0]); assertTrue(store.list().isEmpty());
    }
    @Test public void stagingFailureStillClosesInput() throws Exception {
        File root = temp.newFolder("unavailable");
        store = new SnapshotStore(root);
        assertTrue(root.delete()); assertTrue(root.createNewFile());
        final boolean[] closed = {false};
        InputStream input = new ByteArrayInputStream(archive("world_data", metadata())) {
            @Override public void close() { closed[0] = true; }
        };
        try { store.importZip(input); fail(); } catch (IOException expected) { assertTrue(expected.getMessage().contains("staging")); }
        assertTrue(closed[0]);
    }

}
