package io.github.realmcraft.companion;
import android.os.ParcelFileDescriptor;
interface IWorldAccess {
    String stopAndListWorlds() = 1;
    ParcelFileDescriptor openWorldZip(String worldId) = 2;
    void destroy() = 16777114;
}
