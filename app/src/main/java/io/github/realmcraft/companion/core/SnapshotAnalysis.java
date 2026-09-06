package io.github.realmcraft.companion.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.regex.Pattern;

/** Reads only bounded files in an imported snapshot and verifies each file's recorded SHA-256. */
public final class SnapshotAnalysis {
    public final List<ChunkSurface> chunks=new ArrayList<>();
    public PlayerReader player;
    public String playerIssue;
    public int skippedChunks, totalChunks;
    public boolean limited,pointsLimited;
    public final List<MapPoint> points=new ArrayList<>();
    public MapOptions options;
    private final SnapshotStore.Snapshot snapshot;
    private final Map<String,String> hashes=new TreeMap<>();
    private final String prefix;
    private SnapshotAnalysis(SnapshotStore.Snapshot snapshot) throws IOException {
        this.snapshot=snapshot;
        Properties p=new Properties();p.load(new ByteArrayInputStream(readBounded(new File(snapshot.directory,"manifest.properties"),32*1024*1024)));
        String metadata=p.getProperty("metadataPath","");safePath(metadata);
        if(!metadata.endsWith("world_data"))throw new IOException("Invalid world metadata path");
        prefix=metadata.substring(0,metadata.length()-"world_data".length());
        try {
            int count=Integer.parseInt(p.getProperty("fileCount"));if(count<1||count>100000)throw new IOException("Invalid snapshot manifest");
            MessageDigest digest=sha();
            for(int i=0;i<count;i++) {
                String path=p.getProperty("file."+i+".path",""),hash=p.getProperty("file."+i+".sha256","");safePath(path);
                if(!hash.matches("[a-f0-9]{64}")||hashes.put(path,hash)!=null)throw new IOException("Invalid snapshot checksum");
            }
            for(Map.Entry<String,String> e:hashes.entrySet()) {digest.update(e.getKey().getBytes(StandardCharsets.UTF_8));digest.update((byte)0);digest.update(e.getValue().getBytes(StandardCharsets.US_ASCII));digest.update((byte)'\n');}
            if(!hex(digest.digest()).equals(snapshot.sha256))throw new IOException("Snapshot manifest checksum mismatch");
        }catch(NumberFormatException e){throw new IOException("Invalid snapshot manifest",e);}
    }
    public static SnapshotAnalysis load(SnapshotStore.Snapshot snapshot) throws IOException {
        return load(snapshot,MapOptions.defaults());
    }
    public static SnapshotAnalysis load(SnapshotStore.Snapshot snapshot,MapOptions options) throws IOException {
        SnapshotAnalysis result=new SnapshotAnalysis(snapshot);result.options=options;
        try {result.player=PlayerReader.parse(result.read(result.prefix+"player_data",4_000_000));}
        catch(IOException e) {if(e instanceof InterruptedIOException)throw e;result.playerIssue=e.getMessage();}
        Pattern chunk=Pattern.compile("[on]\\.-?\\d+,-?\\d+");long processedBytes=0;
        for(String path:result.hashes.keySet()) {
            checkCancelled();if(!path.startsWith(result.prefix))continue;
            String name=path.substring(result.prefix.length());if(!chunk.matcher(name).matches())continue;
            try{String[] c=name.substring(2).split(",");if(!options.includes(Integer.parseInt(c[0]),Integer.parseInt(c[1])))continue;}
            catch(NumberFormatException e){result.skippedChunks++;continue;}
            result.totalChunks++;
            if(result.chunks.size()+result.skippedChunks>=options.chunks||processedBytes>=options.byteLimit){result.limited=true;continue;}
            try {byte[] bytes=result.read(path,4*1024*1024);processedBytes+=bytes.length;ChunkSurface surface=ChunkSurface.decode(bytes,name,options.ceiling);
                for(MapPoint point:surface.points){if(result.points.size()<5000)result.points.add(point);else result.pointsLimited=true;}
                result.pointsLimited|=surface.pointsLimited;surface.points.clear();result.chunks.add(surface);}
            catch(IOException e){if(e instanceof InterruptedIOException)throw e;result.skippedChunks++;}
        }
        return result;
    }
    private byte[] read(String path,int limit) throws IOException {
        checkCancelled();String expected=hashes.get(path);if(expected==null)throw new IOException("File unavailable / Datei nicht vorhanden: "+new File(path).getName());
        safePath(path);byte[] bytes=readBounded(new File(new File(snapshot.directory,"world"),path),limit);
        if(!hex(sha().digest(bytes)).equals(expected))throw new IOException("File checksum mismatch / Datei-Prüfsumme stimmt nicht überein.");return bytes;
    }
    private static void safePath(String p) throws IOException {
        if(p.isEmpty()||p.startsWith("/")||p.contains("\\")||p.contains(":"))throw new IOException("Invalid snapshot path");
        for(String part:p.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IOException("Invalid snapshot path");
    }
    private static byte[] readBounded(File f,int limit) throws IOException {
        if(!f.getCanonicalFile().equals(f.getAbsoluteFile())||!f.isFile()||f.length()>limit)throw new IOException("File unavailable or too large / Datei fehlt oder ist zu groß.");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[32768];
        try(InputStream in=new FileInputStream(f)){int n;while((n=in.read(buffer))!=-1){checkCancelled();if(n>limit-out.size())throw new IOException("File exceeds size limit");out.write(buffer,0,n);}}
        return out.toByteArray();
    }
    private static void checkCancelled() throws InterruptedIOException {if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Analysis cancelled");}
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
}
