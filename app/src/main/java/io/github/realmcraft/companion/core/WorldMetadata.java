package io.github.realmcraft.companion.core;

import java.io.*;
import java.util.Properties;

/** Strict version-9 metadata reader; unknown layouts require explicit support. */
public final class WorldMetadata {
    public final String name, worldId, seed;
    private WorldMetadata(Properties p) { name = p.getProperty("name"); worldId = p.getProperty("worldId"); seed = p.getProperty("seed"); }
    public static WorldMetadata read(File file) throws IOException { return new WorldMetadata(SnapshotStore.parseMetadata(file)); }
}
