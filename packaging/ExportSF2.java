/**
 * One-time asset packaging tool — NOT part of the RuneLite plugin.
 *
 * Reads RS cache indexes 4 and 14 via synth-engine.jar + Displee, scans all
 * MIDI files to find which patches are actually used, and exports a filtered
 * SF2 containing only those patches.  The result is far smaller than a full
 * export and safe to load at plugin runtime.
 *
 * Run from the project root:
 *   javac -cp "libs/synth-engine.jar;libs/Displee-RS-Cache-Library-readonly.jar" ^
 *         packaging/ExportSF2.java -d packaging/out
 *   java --add-opens java.desktop/com.sun.media.sound=ALL-UNNAMED ^
 *        -cp "packaging/out;libs/synth-engine.jar;libs/Displee-RS-Cache-Library-readonly.jar" ^
 *        ExportSF2 <cache-dir> <midi-dir> <fanfare-dir> <output.sf2>
 */

import javax.sound.midi.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

public class ExportSF2 {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: ExportSF2 <cache-dir> <midi-dir> <fanfare-dir> <output.sf2>");
            System.exit(1);
        }
        Path cacheDir   = Path.of(args[0]);
        Path midiDir    = Path.of(args[1]);
        Path fanfareDir = Path.of(args[2]);
        Path outFile    = Path.of(args[3]);

        // Scan all MIDI files and collect used program numbers
        Set<Integer> usedPrograms = new HashSet<>();
        int midiCount = 0;
        for (Path dir : new Path[]{midiDir, fanfareDir}) {
            if (!Files.isDirectory(dir)) continue;
            try (var files = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".mid"))::iterator) {
                    scanMidi(p, usedPrograms);
                    midiCount++;
                }
            }
        }
        System.out.println("Scanned " + midiCount + " MIDI files, found programs: " + new TreeSet<>(usedPrograms));

        // Open RS cache via Displee
        System.out.println("Opening cache: " + cacheDir);
        Class<?> libClass = Class.forName("org.displee.CacheLibrary");
        Object lib = libClass.getMethod("createUncached", String.class)
                             .invoke(null, cacheDir.toString() + File.separator);

        Class<?> indexClass = Class.forName("org.displee.cache.index.Index");
        Object idx4  = libClass.getMethod("getIndex", int.class).invoke(lib, 4);
        Object idx14 = libClass.getMethod("getIndex", int.class).invoke(lib, 14);

        // Construct package-private SoundBankCache via reflection
        Class<?> sbcClass  = Class.forName("main.SoundBankCache");
        Constructor<?> sbc = sbcClass.getDeclaredConstructor(indexClass, indexClass);
        sbc.setAccessible(true);
        Object soundBankCache = sbc.newInstance(idx4, idx14);

        // Set up MakeSoundFont
        Class<?> msfClass = Class.forName("main.MakeSoundFont");
        Class<?> patchClass = Class.forName("main.MusicPatch");
        Object maker = msfClass.getDeclaredConstructor().newInstance();
        msfClass.getMethod("initSoundFont").invoke(maker);

        Method getPatch = patchClass.getMethod("getMusicPatch", indexClass, int.class, int.class);
        Method makeSF   = msfClass.getMethod("makeSoundFont", patchClass, sbcClass, int.class);

        Object[] archives = (Object[]) indexClass.getMethod("getArchives").invoke(idx4);
        int total = archives.length;
        System.out.println("Processing " + total + " patch archives (filtering to " + usedPrograms.size() + " used)...");

        int loaded = 0;
        for (int i = 0; i < total; i++) {
            if (!usedPrograms.contains(i % 128)) continue;  // skip unused programs
            try {
                Object patch = getPatch.invoke(null, idx4, i, 0);
                if (patch != null) {
                    makeSF.invoke(maker, patch, soundBankCache, i);
                    loaded++;
                }
            } catch (Exception e) {
                // skip bad/empty patches silently
            }
        }
        System.out.println("Loaded " + loaded + " patches.");

        // Save the filtered SF2
        Field sf2Field = msfClass.getDeclaredField("sf2Soundbank");
        sf2Field.setAccessible(true);
        Object sf2Soundbank = sf2Field.get(maker);

        Class<?> sf2Class = Class.forName("com.sun.media.sound.SF2Soundbank");
        try (OutputStream os = Files.newOutputStream(outFile)) {
            sf2Class.getMethod("save", OutputStream.class).invoke(sf2Soundbank, os);
        }

        long bytes = Files.size(outFile);
        System.out.printf("Wrote %s (%.1f MB)%n", outFile, bytes / 1_048_576.0);

        libClass.getMethod("close").invoke(lib);
    }

    private static void scanMidi(Path file, Set<Integer> programs) {
        try {
            Sequence seq = MidiSystem.getSequence(file.toFile());
            for (Track track : seq.getTracks()) {
                for (int i = 0; i < track.size(); i++) {
                    MidiEvent  event   = track.get(i);
                    MidiMessage msg    = event.getMessage();
                    if (msg instanceof ShortMessage sm) {
                        if (sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                            programs.add(sm.getData1());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("WARN: could not parse " + file + ": " + e.getMessage());
        }
    }
}
