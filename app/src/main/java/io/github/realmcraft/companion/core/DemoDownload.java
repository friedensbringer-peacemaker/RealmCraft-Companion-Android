package io.github.realmcraft.companion.core;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.Locale;

/** Explicit, bounded public demo download. Never reads or uploads device data. */
public final class DemoDownload {
    public static final String REPOSITORY = "https://github.com/friedensbringer-peacemaker/RealmCraft-Companion-Android";
    public static final String ZIP_URL = REPOSITORY + "/releases/download/demo-world-v2/RealmCraft-Companion-Demo.zip";
    public static final String SHA256 = "6b2cfdf2bc678b70f61d223d04ced557e0d9565a7d3ef6fe66112f49ce5cfae1";
    private static final int MAX_BYTES = 4 * 1024 * 1024;
    private DemoDownload() { }

    public static byte[] download() throws IOException {
        URL url = new URL(ZIP_URL);
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!allowed(url)) throw new IOException("Unsupported demo download destination.");
            checkCancelled();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000); connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "RealmCraft-Companion-Android");
            connection.setRequestProperty("Accept", "application/octet-stream");
            try {
                int code = connection.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Missing demo redirect destination.");
                    url = new URL(url, location); continue;
                }
                if (code != 200) throw new IOException("Demo download failed (HTTP " + code + "). Please try again later.");
                if (connection.getContentLengthLong() > MAX_BYTES) throw new IOException("Demo archive exceeds download limit.");
                try (InputStream input = connection.getInputStream()) { return verifiedBytes(input, SHA256, MAX_BYTES); }
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many demo download redirects.");
    }
    static boolean allowed(URL url) {
        String host = url.getHost().toLowerCase(Locale.ROOT);
        return url.getProtocol().equals("https") && url.getUserInfo() == null && (url.getPort() == -1 || url.getPort() == 443)
            && (host.equals("github.com") || host.equals("release-assets.githubusercontent.com") || host.equals("objects.githubusercontent.com"));
    }
    static byte[] verifiedBytes(InputStream input, String expected, int limit) throws IOException {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[32768];
        while (true) {
            checkCancelled(); int count = input.read(buffer); checkCancelled();
            if (count < 0) break;
            if (count > limit - output.size()) throw new IOException("Demo archive exceeds download limit.");
            output.write(buffer, 0, count); digest.update(buffer, 0, count);
        }
        StringBuilder actual = new StringBuilder();
        for (byte value : digest.digest()) actual.append(String.format(Locale.ROOT, "%02x", value & 255));
        if (!actual.toString().equals(expected)) throw new IOException("Demo checksum mismatch. Nothing was imported.");
        return output.toByteArray();
    }
    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Demo download cancelled.");
    }
}
