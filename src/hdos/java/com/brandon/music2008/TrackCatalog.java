package com.brandon.music2008;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Matches cache name hashes rather than assuming archive IDs are shared. */
public final class TrackCatalog {
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private final Map<Integer, Integer> archives = new HashMap<>();
    private final Set<Integer> ambiguous = new HashSet<>();
    private final Map<Integer, Path> midiFiles = new HashMap<>();

    public static int nameHash(String name) {
        String clean = name.replaceAll("<[^>]*>", "").replace(' ', ' ').trim().toLowerCase(Locale.ROOT);
        int hash = 0;
        for (byte b : clean.getBytes(CP1252)) hash = hash * 31 + b;
        return hash;
    }

    public void add(int archive, int hash) {
        if (hash == 0 || hash == -1 || ambiguous.contains(hash)) return;
        Integer previous = archives.putIfAbsent(hash, archive);
        if (previous != null && previous != archive) { archives.remove(hash); ambiguous.add(hash); }
    }

    public static TrackCatalog load(Path midiDir) throws IOException {
        TrackCatalog catalog = new TrackCatalog();
        try (InputStream in = TrackCatalog.class.getResourceAsStream("/2008-music-index.tsv")) {
            if (in == null) throw new IOException("Missing 2008 Music Soundtrack catalogue");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#") || line.isBlank()) continue;
                    String[] parts = line.split("\t");
                    catalog.add(Integer.parseInt(parts[0]), (int) Long.parseLong(parts[1]));
                }
            }
        }
        try (Stream<Path> paths = Files.list(midiDir)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                String filename = path.getFileName().toString();
                if (!filename.toLowerCase(Locale.ROOT).endsWith(".mid")) continue;
                try {
                    int archive = Integer.parseInt(filename.split("[ .-]", 2)[0]);
                    if (catalog.midiFiles.putIfAbsent(archive, path) != null)
                        throw new IOException("Duplicate MIDI archive " + archive + " in " + midiDir);
                } catch (NumberFormatException ignored) { }
            }
        }
        return catalog;
    }
    public Integer archiveFor(String name) { return archives.get(nameHash(name)); }
    public Path midiFor(String name) { Integer archive = archiveFor(name); return archive == null ? null : midiFiles.get(archive); }
    public Path midiFor(int archive) { return midiFiles.get(archive); }
    public int available() { return midiFiles.size(); }
}
