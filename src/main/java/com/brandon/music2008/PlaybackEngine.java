package com.brandon.music2008;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MIDI playback using the bundled SF2 soundfont via the JDK's built-in synthesizer. */
final class PlaybackEngine implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PlaybackEngine.class);

    static final class Request {
        final String name; final Path soundbank; final Path midi; final boolean loop; final long revision;
        Request(String name, Path soundbank, Path midi, boolean loop, long revision) {
            this.name = name; this.soundbank = soundbank; this.midi = midi; this.loop = loop; this.revision = revision;
        }
        public boolean equals(Object o) {
            if (!(o instanceof Request)) return false;
            Request r = (Request) o;
            return revision == r.revision && loop == r.loop && name.equals(r.name) && soundbank.equals(r.soundbank) && midi.equals(r.midi);
        }
        public int hashCode() { return Long.hashCode(revision); }
        // accessors
        String name() { return name; }
        Path soundbank() { return soundbank; }
        Path midi() { return midi; }
        boolean loop() { return loop; }
    }

    private final Object wake = new Object();
    private final AtomicReference<Request> desired = new AtomicReference<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "music2008-audio");
        t.setDaemon(true);
        return t;
    });

    private volatile Request active;
    private volatile String status = "Waiting for a song";
    private volatile double volume = 1.0;
    private volatile boolean closed;

    // Synthesizer and soundbank are created once and reused across tracks
    private Synthesizer synthesizer;
    private Soundbank   soundbank;
    private Path        loadedSoundbankPath;

    PlaybackEngine() { worker.submit(this::run); }

    void request(Request request) {
        desired.set(request);
        synchronized (wake) { wake.notifyAll(); }
    }

    void setVolume(double value) {
        volume = Math.max(0, Math.min(1, value));
        applyVolume();
    }

    boolean isActive(Request request) { return request != null && request.equals(active); }
    String status() { return status; }

    private void applyVolume() {
        Synthesizer s = synthesizer;
        if (s == null || !s.isOpen()) return;
        int v = (int) (volume * 127);
        for (MidiChannel ch : s.getChannels()) {
            if (ch != null) ch.controlChange(7, v); // CC7 = channel volume
        }
    }

    private void run() {
        Request last = null;
        while (!closed) {
            Request next = desired.get();
            if (next == null || next.equals(last)) {
                if (next == null) { last = null; active = null; }
                synchronized (wake) {
                    try { wake.wait(50); } catch (InterruptedException e) { break; }
                }
                continue;
            }
            last = next;
            status = "Loading: " + next.name();
            log.info("Loading {}", next.name());
            try {
                ensureSynthesizer(next.soundbank());
                do {
                    if (closed || !next.equals(desired.get())) break;
                    playOnce(next);
                } while (!closed && next.loop() && next.equals(desired.get()));

                if (!closed) status = "Waiting for the next song";
            } catch (Exception e) {
                active = null;
                status = "Unavailable; original music retained: " + e.getClass().getSimpleName();
                log.warn("Playback failed for {}; retaining original music", next.name(), e);
            }
        }
        active = null;
        closeSynthesizer();
    }

    private void ensureSynthesizer(Path sbPath) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        if (synthesizer != null && synthesizer.isOpen() && sbPath.equals(loadedSoundbankPath)) return;
        closeSynthesizer();

        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();

        soundbank = MidiSystem.getSoundbank(sbPath.toFile());
        synthesizer.loadAllInstruments(soundbank);
        loadedSoundbankPath = sbPath;
        applyVolume();
        log.info("Synthesizer opened with soundbank: {}", sbPath.getFileName());
    }

    private void playOnce(Request next) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        Sequence seq = MidiSystem.getSequence(next.midi().toFile());

        try (Sequencer sequencer = MidiSystem.getSequencer(false)) {
            sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
            sequencer.open();
            sequencer.setSequence(seq);
            sequencer.setLoopCount(0);
            sequencer.start();

            if (closed || !next.equals(desired.get())) {
                sequencer.stop();
                return;
            }
            active = next;
            status = "Playing: " + next.name();
            log.info("Playing {}", next.name());

            while (!closed && next.equals(desired.get()) && sequencer.isRunning()) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
            }
            sequencer.stop();
            active = null;
        }
    }

    private void closeSynthesizer() {
        if (synthesizer != null) {
            if (soundbank != null) {
                try { synthesizer.unloadAllInstruments(soundbank); } catch (Exception ignored) {}
            }
            if (synthesizer.isOpen()) synthesizer.close();
            synthesizer = null;
            soundbank   = null;
            loadedSoundbankPath = null;
        }
    }

    @Override
    public void close() {
        closed = true;
        desired.set(null);
        synchronized (wake) { wake.notifyAll(); }
        worker.shutdownNow();
        closeSynthesizer();
    }
}
