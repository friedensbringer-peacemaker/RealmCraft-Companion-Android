package io.github.realmcraft.companion;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;
import io.github.realmcraft.companion.core.WorldMetadata;

/** Runs in Shizuku's shell process. Only reads the fixed RealmCraft save root. */
public final class WorldAccessService extends IWorldAccess.Stub {
    private static final File ROOT = new File("/sdcard/Android/data/com.TellurionMobile.RealmCraft/files/local");
    private static final String PACKAGE = "com.TellurionMobile.RealmCraft";
    private static final long MAX_BYTES = 1024L * 1024 * 1024;
    private static final int MAX_FILES = 100000;

    public WorldAccessService() { }

    @Override public synchronized String stopAndListWorlds() throws RemoteException {
        try {
            stopGame();
            File[] folders = ROOT.listFiles();
            if (folders == null) throw new IOException("Save folder is unavailable. Check Shizuku access on this device.");
            Arrays.sort(folders, Comparator.comparing(File::getName));
            JSONArray result = new JSONArray();
            for (File folder : folders) {
                if (!folder.getName().matches("[0-9]{1,10}") || !folder.isDirectory()) continue;
                File world = worldFolder(folder.getName());
                List<File> files = inventory(world);
                long size = 0;
                for (File file : files) size += file.length();
                JSONObject info = new JSONObject();
                info.put("id", folder.getName());
                info.put("name", "Unknown world");
                info.put("seed", "?");
                File metadata = new File(world, "world_data");
                info.put("modified", metadata.lastModified());
                info.put("bytes", size);
                info.put("files", files.size());
                try {
                    WorldMetadata parsed = WorldMetadata.read(metadata);
                    info.put("name", parsed.name);
                    info.put("seed", parsed.seed);
                } catch (IOException unsupported) {
                    info.put("name", "Unsupported world metadata");
                }
                result.put(info);
            }
            return result.toString();
        } catch (Exception error) {
            throw new RemoteException("Cannot list worlds: " + error.getMessage());
        }
    }

    @Override public synchronized ParcelFileDescriptor openWorldZip(String worldId) throws RemoteException {
        try {
            File world = worldFolder(worldId);
            stopGame();
            List<File> files = inventory(world);
            if (!new File(world, "world_data").isFile()) throw new IOException("Missing world_data.");
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createReliablePipe();
            new Thread(() -> streamWorld(world, files, pipe[1]), "world-snapshot").start();
            return pipe[0];
        } catch (Exception error) {
            throw new RemoteException("Cannot import world: " + error.getMessage());
        }
    }

    private static File worldFolder(String id) throws IOException {
        if (id == null || !id.matches("[0-9]{1,10}")) throw new IOException("Invalid world selection.");
        File root = ROOT.getCanonicalFile();
        File folder = new File(root, id);
        if (Files.isSymbolicLink(folder.toPath()) || !folder.getCanonicalFile().equals(folder) || !folder.isDirectory()) {
            throw new IOException("World is not a regular folder.");
        }
        return folder;
    }

    private static List<File> inventory(File world) throws IOException {
        List<File> files = new ArrayList<>();
        collect(world, world, files, 0, new long[]{0, 0});
        files.sort(Comparator.comparing(File::getPath));
        return files;
    }

    private static void collect(File root, File folder, List<File> files, int depth, long[] totals) throws IOException {
        if (depth > 32) throw new IOException("Folder nesting exceeds prototype limit.");
        File[] children = folder.listFiles();
        if (children == null) throw new IOException("Cannot read a world folder.");
        for (File child : children) {
            if (++totals[1] > MAX_FILES) throw new IOException("World contains too many entries.");
            if (Files.isSymbolicLink(child.toPath()) || !child.getCanonicalPath().startsWith(root.getPath() + File.separator)) {
                throw new IOException("Linked files are not supported.");
            }
            if (child.isDirectory()) collect(root, child, files, depth + 1, totals);
            else if (child.isFile()) {
                totals[0] += child.length();
                if (totals[0] > MAX_BYTES) throw new IOException("World exceeds the 1 GiB prototype limit.");
                files.add(child);
            } else throw new IOException("Unsupported file type.");
        }
    }

    private static void streamWorld(File world, List<File> files, ParcelFileDescriptor output) {
        ZipOutputStream zip = null;
        try {
            OutputStream raw = new ParcelFileDescriptor.AutoCloseOutputStream(output);
            zip = new ZipOutputStream(new BufferedOutputStream(raw));
            Map<String, String> signatures = new HashMap<>();
            byte[] buffer = new byte[65536];
            long total = 0;
            for (File file : files) {
                if (Files.isSymbolicLink(file.toPath()) || !file.getCanonicalPath().startsWith(world.getCanonicalPath() + File.separator)) throw new IOException("Source changed during import.");
                long length = file.length(), modified = file.lastModified();
                String relative = world.toPath().relativize(file.toPath()).toString();
                signatures.put(relative, length + ":" + modified);
                ZipEntry entry = new ZipEntry(world.getName() + "/" + relative);
                entry.setTime(modified);
                zip.putNextEntry(entry);
                long copied = 0;
                try (InputStream input = new FileInputStream(file)) {
                    int n;
                    while ((n = input.read(buffer)) != -1) {
                        total += n; copied += n;
                        if (total > MAX_BYTES) throw new IOException("World exceeds import limit.");
                        zip.write(buffer, 0, n);
                    }
                }
                zip.closeEntry();
                if (copied != length || file.length() != length || file.lastModified() != modified) {
                    throw new IOException("RealmCraft changed a file. Close the game and retry.");
                }
            }
            List<File> after = inventory(world);
            if (after.size() != files.size()) throw new IOException("World changed during import.");
            for (File file : after) {
                String relative = world.toPath().relativize(file.toPath()).toString();
                if (!(file.length() + ":" + file.lastModified()).equals(signatures.get(relative))) {
                    throw new IOException("World changed during import.");
                }
            }
            ensureGameStopped();
            zip.finish();
            zip.close();
        } catch (Exception error) {
            try { output.closeWithError("Snapshot failed: " + error.getMessage()); }
            catch (IOException ignored) { }
        } finally {
            if (zip != null) try { zip.close(); } catch (IOException ignored) { }
        }
    }

    private static void stopGame() throws Exception {
        Process stop = new ProcessBuilder("/system/bin/am", "force-stop", PACKAGE).redirectErrorStream(true).start();
        if (!stop.waitFor(10, TimeUnit.SECONDS)) {
            stop.destroyForcibly();
            throw new IOException("Stopping RealmCraft timed out.");
        }
        if (stop.exitValue() != 0) throw new IOException("Unable to stop RealmCraft with this Shizuku permission.");
        ensureGameStopped();
    }

    private static void ensureGameStopped() throws Exception {
        Process probe = new ProcessBuilder("/system/bin/pidof", PACKAGE).redirectErrorStream(true).start();
        if (!probe.waitFor(5, TimeUnit.SECONDS)) {
            probe.destroyForcibly();
            throw new IOException("Could not verify whether RealmCraft is stopped.");
        }
        if (probe.exitValue() == 0) throw new IOException("RealmCraft is running. Close it and retry.");
        if (probe.exitValue() != 1) throw new IOException("Game-state check is unavailable on this device.");
    }

    @Override public void destroy() { System.exit(0); }
}
