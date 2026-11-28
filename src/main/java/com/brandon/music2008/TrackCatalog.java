package com.brandon.music2008;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import net.runelite.client.util.Filepath;

/** Matches cache name hashes rather than assuming archive IDs are shared. */
public final class TrackCatalog {
    private static final Charset CP1252 = Charset.forName("windows-1252");
    private final Map<Integer, Integer> archives = new HashMap<>();
    private final Set<Integer> ambiguous = new HashSet<>();
    private final Map<Integer, Filepath> midiFiles = new HashMap<>();

    public static int nameHash(String name) {
        String clean = name.replaceAll("<[^>]*>", "").replace(' ', ' ').trim().toLowerCase(Locale.ROOT);
        int hash = 0;
        for (byte b : clean.getBytes(CP1252)) hash = hash * 31 + b;
        return hash;
    }

    public void add(int archive, int hash) {
        if (hash == 0 || hash == -1 || ambiguous.contains(hash)) return;
        Integer previous = archives.putIfAbsent(hash, archive);
        if (previous != null && previous != archive) { archives.remove(hash); ambiguous.add(hash); }
    }

    public static TrackCatalog load(Filepath midiDir) throws IOException {
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
        if (midiDir.isDirectory()) {
            try (var stream = midiDir.walk(1)) {
                for (var it = stream.iterator(); it.hasNext(); ) {
                    Filepath fp = it.next();
                    if (!fp.isFile()) continue;
                    String filename = fp.getFileName();
                    if (!filename.toLowerCase(Locale.ROOT).endsWith(".mid")) continue;
                    try {
                        int archive = Integer.parseInt(filename.split("[ .-]", 2)[0]);
                        if (catalog.midiFiles.putIfAbsent(archive, fp) != null)
                            throw new IOException("Duplicate MIDI archive " + archive + " in " + midiDir);
                    } catch (NumberFormatException ignored) { }
                }
            }
        }
        return catalog;
    }

    public Integer archiveFor(String name) { return archives.get(nameHash(name)); }
    public Filepath midiFor(String name) { Integer archive = archiveFor(name); return archive == null ? null : midiFiles.get(archive); }
    public Filepath midiFor(int archive) { return midiFiles.get(archive); }
    public int available() { return midiFiles.size(); }

    /**
     * Load fanfare MIDI files from a directory.
     * Fanfare filenames follow the same convention: leading archive ID, e.g. "245 - Levelup.mid".
     * No name-hash index is used because fanfares are triggered by OSRS script/event, not by
     * displayed name. The caller resolves the fanfare archive by OSRS fanfare ID once a
     * confirmed script-ID-to-archive mapping is available.
     */
    public static Map<Integer, Filepath> loadFanfares(Filepath fanfareDir) throws IOException {
        Map<Integer, Filepath> fanfares = new LinkedHashMap<>();
        if (!fanfareDir.isDirectory()) return fanfares;
        try (var stream = fanfareDir.walk(1)) {
            for (var it = stream.iterator(); it.hasNext(); ) {
                Filepath fp = it.next();
                if (!fp.isFile()) continue;
                String filename = fp.getFileName();
                if (!filename.toLowerCase(Locale.ROOT).endsWith(".mid")) continue;
                try {
                    int archive = Integer.parseInt(filename.split("[ .-]", 2)[0]);
                    fanfares.putIfAbsent(archive, fp);
                } catch (NumberFormatException ignored) {}
            }
        }
        return Collections.unmodifiableMap(fanfares);
    }
}
