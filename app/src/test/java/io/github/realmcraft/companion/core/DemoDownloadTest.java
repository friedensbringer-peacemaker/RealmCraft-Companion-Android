package io.github.realmcraft.companion.core;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.net.URL;

public final class DemoDownloadTest {
    private static final String HELLO = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
    @Test public void acceptsOnlyExactBytes() throws Exception {
        assertArrayEquals("hello".getBytes("UTF-8"), DemoDownload.verifiedBytes(new ByteArrayInputStream("hello".getBytes("UTF-8")), HELLO, 5));
    }
    @Test public void rejectsWrongDigestAndOversize() throws Exception {
        for (int limit : new int[]{4, 20}) {
            try { DemoDownload.verifiedBytes(new ByteArrayInputStream("changed".getBytes("UTF-8")), HELLO, limit); fail("Invalid download accepted"); }
            catch (IOException expected) { }
        }
    }
    @Test public void rejectsUnsafeRedirects() throws Exception {
        assertTrue(DemoDownload.allowed(new URL(DemoDownload.ZIP_URL)));
        assertTrue(DemoDownload.allowed(new URL("https://release-assets.githubusercontent.com/test")));
        for (String target : new String[]{"http://github.com/test", "https://github.com.evil.example/test", "https://evil.example/test", "https://name@github.com/test", "https://github.com:444/test"}) assertFalse(DemoDownload.allowed(new URL(target)));
    }
    @Test public void interruptionCancelsBeforeReading() throws Exception {
        Thread.currentThread().interrupt();
        try { DemoDownload.verifiedBytes(new ByteArrayInputStream(new byte[0]), HELLO, 20); fail("Cancellation ignored"); }
        catch (InterruptedIOException expected) { }
        finally { Thread.interrupted(); }
    }
}
