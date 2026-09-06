package io.github.realmcraft.companion.core;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Immutable, read-only world copies. Never opens the game's storage directory. */
public final class SnapshotStore {
    private final File root;
    private final long limit;
    private final int entryLimit;
    public SnapshotStore(File libraryRoot) throws IOException { this(libraryRoot, 1024L * 1024 * 1024, 100000); }
    public SnapshotStore(File libraryRoot, long maxBytes, int maxEntries) throws IOException {
        if (maxBytes < 1 || maxEntries < 1) throw new IllegalArgumentException("Invalid import limits");
        root = libraryRoot.getCanonicalFile(); limit = maxBytes; entryLimit = maxEntries;
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("Cannot create snapshot library");
    }
    public static final class Snapshot {
        public final String id, name, worldId, seed, sha256;
        public final long bytes, fileCount, importedAt;
        public final File directory;
        public final boolean synthetic;
        public final List<String> files;
        private Snapshot(File directory, Properties p) {
            this.directory = directory; id = directory.getName(); name = p.getProperty("name");
            worldId = p.getProperty("worldId"); seed = p.getProperty("seed"); sha256 = p.getProperty("sha256");
            bytes = Long.parseLong(p.getProperty("bytes")); fileCount = Long.parseLong(p.getProperty("fileCount"));
            importedAt = Long.parseLong(p.getProperty("importedAt")); synthetic = Boolean.parseBoolean(p.getProperty("synthetic"));
            ArrayList<String> paths = new ArrayList<>();
            for (int i = 0; i < fileCount; i++) paths.add(p.getProperty("file." + i + ".path"));
            files = Collections.unmodifiableList(paths);
        }
    }
    public Snapshot importZip(InputStream source) throws IOException { return importZip(source, false); }
    private Snapshot importZip(InputStream source, boolean synthetic) throws IOException {
        if (source == null) throw new IOException("No archive input provided");
        String id = UUID.randomUUID().toString();
        File stage = new File(root, ".staging-" + id), payload = new File(stage, "world");
        try {
            File archive = new File(stage, "input.zip");
            try (InputStream incoming = source) {
                checkCancelled();
                if (!payload.mkdirs()) throw new IOException("Cannot create import staging directory");
                try (OutputStream out = new FileOutputStream(archive)) { copy(incoming, out, limit, null, null); }
            }
            checkCancelled();
            TreeMap<String, String> hashes = new TreeMap<>();
            Set<String> names = new HashSet<>();
            File metadata = null;
            long total = 0; int entries = 0;
            // ZipFile requires a complete central directory; ZipInputStream alone accepts truncated archives.
            try (ZipFile zip = new ZipFile(archive)) {
                Enumeration<? extends ZipEntry> all = zip.entries();
                while (all.hasMoreElements()) {
                    checkCancelled();
                    ZipEntry entry = all.nextElement();
                    if (++entries > entryLimit) throw new IOException("Archive has too many entries");
                    String path = validatePath(entry.getName(), entry.isDirectory());
                    if (!names.add(path)) throw new IOException("Duplicate archive path: " + path);
                    File target = new File(payload, path);
                    if (!target.getCanonicalPath().startsWith(payload.getCanonicalPath() + File.separator)) throw new IOException("Unsafe archive path");
                    if (entry.isDirectory()) {
                        if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Conflicting archive path");
                        continue;
                    }
                    if (target.exists()) throw new IOException("Conflicting archive path");
                    if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) throw new IOException("Conflicting archive path");
                    MessageDigest digest = digest(); CRC32 crc = new CRC32(); long count;
                    try (InputStream in = zip.getInputStream(entry); OutputStream out = new FileOutputStream(target)) {
                        count = copy(in, out, limit - total, digest, crc);
                    }
                    if (entry.getSize() != count || entry.getCrc() != crc.getValue()) throw new IOException("Archive entry failed integrity check");
                    total += count; hashes.put(path, hex(digest.digest()));
                    if (target.getName().equals("world_data")) {
                        if (metadata != null) throw new IOException("Archive contains multiple worlds. Export and import one world at a time.");
                        metadata = target;
                    }
                }
            }
            if (metadata == null) throw new IOException("No world_data found in archive");
            Properties p = parseMetadata(metadata);
            p.setProperty("bytes", Long.toString(total)); p.setProperty("fileCount", Integer.toString(hashes.size()));
            p.setProperty("importedAt", Long.toString(System.currentTimeMillis())); p.setProperty("synthetic", Boolean.toString(synthetic));
            p.setProperty("metadataPath", payload.toURI().relativize(metadata.toURI()).getPath());
            MessageDigest combined = digest(); int i = 0;
            for (Map.Entry<String, String> item : hashes.entrySet()) {
                p.setProperty("file." + i + ".path", item.getKey()); p.setProperty("file." + i + ".sha256", item.getValue()); i++;
                combined.update(item.getKey().getBytes(StandardCharsets.UTF_8)); combined.update((byte) 0);
                combined.update(item.getValue().getBytes(StandardCharsets.US_ASCII)); combined.update((byte) '\n');
            }
            p.setProperty("sha256", hex(combined.digest()));
            try (FileOutputStream out = new FileOutputStream(new File(stage, "manifest.properties"))) { p.store(out, "RealmCraft Companion snapshot format 1"); out.getFD().sync(); }
            if (!archive.delete()) throw new IOException("Cannot remove temporary archive");
            File destination = new File(root, id);
            checkCancelled();
            if (destination.exists() || !stage.renameTo(destination)) throw new IOException("Cannot finalize snapshot");
            return new Snapshot(destination, p);
        } finally { removeTree(stage); }
    }
    private static String validatePath(String path, boolean directory) throws IOException {
        if (path.length() > 2048 || path.startsWith("/") || path.contains("\\") || path.contains(":")) throw new IOException("Unsafe archive path");
        if (directory && path.endsWith("/")) path = path.substring(0, path.length() - 1);
        String[] parts = path.split("/", -1);
        if (parts.length > 32) throw new IOException("Archive path is too deep");
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new IOException("Unsafe archive path");
            for (int i = 0; i < part.length(); i++) if (Character.isISOControl(part.charAt(i))) throw new IOException("Unsafe archive path");
        }
        return path;
    }
    static Properties parseMetadata(File file) throws IOException {
        if (file.length() < 122 || file.length() > 1024 * 1024) throw new IOException("Invalid world_data length");
        byte[] data = new byte[(int) file.length()];
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) { in.readFully(data); }
        ByteBuffer b = ByteBuffer.wrap(data);
        if ((b.get(0) & 255) != 9) throw new IOException("Unsupported world_data version; prototype supports version 9 only");
        int length = b.getInt(13);
        if (length < 1 || length > 4096 || data.length != 17 + length + 105) throw new IOException("Invalid or unsupported world_data structure");
        String name;
        try { name = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data, 17, length)).toString(); }
        catch (CharacterCodingException e) { throw new IOException("Invalid world name encoding", e); }
        Properties p = new Properties(); p.setProperty("name", name); p.setProperty("worldId", Integer.toUnsignedString(b.getInt(1))); p.setProperty("seed", Integer.toString(b.getInt(9))); return p;
    }
    public List<Snapshot> list() throws IOException {
        ArrayList<Snapshot> result = new ArrayList<>(); File[] children = root.listFiles();
        if (children == null) throw new IOException("Cannot read snapshot library");
        for (File dir : children) {
            if (!dir.getName().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") || !dir.isDirectory() || !dir.getCanonicalFile().equals(dir)) continue;
            File manifest = new File(dir, "manifest.properties");
            if (!manifest.isFile() || manifest.length() > 32L * 1024 * 1024 || !manifest.getCanonicalFile().equals(manifest)) continue;
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(manifest)) {
                p.load(in); long count = Long.parseLong(p.getProperty("fileCount")); if (count < 1 || count > entryLimit) continue;
                result.add(new Snapshot(dir, p));
            } catch (IOException | RuntimeException ignored) { /* Incomplete/foreign entries are never presented as imports. */ }
        }
        Collections.sort(result, (a, b) -> Long.compare(b.importedAt, a.importedAt)); return result;
    }
    /** Delete only the explicitly selected app-owned snapshot; never follow links. */
    public void delete(String id) throws IOException {
        if(id==null || !id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) throw new IOException("Invalid snapshot ID");
        File directory=new File(root,id);
        if(!directory.getCanonicalFile().equals(directory.getAbsoluteFile()) || !directory.isDirectory()) throw new IOException("Snapshot unavailable");
        java.nio.file.Files.walkFileTree(directory.toPath(),new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
            @Override public java.nio.file.FileVisitResult visitFile(java.nio.file.Path p,java.nio.file.attribute.BasicFileAttributes a)throws IOException {java.nio.file.Files.delete(p);return java.nio.file.FileVisitResult.CONTINUE;}
            @Override public java.nio.file.FileVisitResult postVisitDirectory(java.nio.file.Path p,IOException e)throws IOException {if(e!=null)throw e;java.nio.file.Files.delete(p);return java.nio.file.FileVisitResult.CONTINUE;}
        });
    }
    public Snapshot createDemo() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            byte[] name = "Synthetic test world".getBytes(StandardCharsets.UTF_8);
            ByteBuffer data = ByteBuffer.allocate(17 + name.length + 105); data.put((byte) 9); data.putInt(42); data.position(9); data.putInt(12345); data.putInt(name.length); data.put(name);
            zip.putNextEntry(new ZipEntry("world_data")); zip.write(data.array()); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("SYNTHETIC-NOT-PLAYABLE.txt")); zip.write("Generated test metadata, terrain and player records only. This is not a playable savegame.".getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("o.0,0")); zip.write(SyntheticFeatureData.chunkWithPoints()); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("o.-16,0")); zip.write(SyntheticFeatureData.chunk(-16,0,0)); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("n.0,0")); zip.write(SyntheticFeatureData.chunk(0,0,1)); zip.closeEntry();
            zip.putNextEntry(new ZipEntry("player_data")); zip.write(SyntheticFeatureData.player()); zip.closeEntry();
        }
        return importZip(new ByteArrayInputStream(bytes.toByteArray()), true);
    }
    private static long copy(InputStream in, OutputStream out, long max, MessageDigest digest, CRC32 crc) throws IOException {
        byte[] buffer = new byte[32768]; long total = 0; int read;
        while (true) { checkCancelled(); read = in.read(buffer); checkCancelled(); if (read == -1) break; if (read == 0) continue; if (read > max - total) throw new IOException("Archive exceeds import size limit"); total += read; out.write(buffer, 0, read); if (digest != null) digest.update(buffer, 0, read); if (crc != null) crc.update(buffer, 0, read); }
        return total;
    }
    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Snapshot import was cancelled");
    }
    private static MessageDigest digest() { try { return MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); } }
    private static String hex(byte[] bytes) { StringBuilder out = new StringBuilder(); for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 255)); return out.toString(); }
    private static void removeTree(File file) {
        try {
            // Do not follow a link even if another process changed staging during cleanup.
            if (!file.getCanonicalFile().equals(file.getAbsoluteFile())) { file.delete(); return; }
            File[] children = file.listFiles();
            if (children != null) for (File child : children) removeTree(child);
            file.delete();
        } catch (IOException ignored) { /* A failed staging directory is never listed as a snapshot. */ }
    }
}
