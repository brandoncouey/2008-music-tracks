package com.brandon.music2008;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.*;
import net.runelite.client.util.Filepath;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** MIDI playback using the bundled SF2 soundfont via the JDK's built-in synthesizer. */
final class PlaybackEngine implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PlaybackEngine.class);

    static final class Request {
        final String name; final Filepath soundbank; final Filepath midi; final boolean loop; final long revision;
        Request(String name, Filepath soundbank, Filepath midi, boolean loop, long revision) {
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
        Filepath soundbank() { return soundbank; }
        Filepath midi() { return midi; }
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
    private double appliedVolume = -1;

    // Synthesizer and soundbank are created once and reused across tracks
    private Synthesizer synthesizer;
    private Soundbank   soundbank;
    private Filepath    loadedSoundbankPath;

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

    /**
     * Universal real-time "master volume" SysEx (F0 7F 7F 04 01 lsb msb F7). Scales the synthesizer's
     * output without touching per-channel CC7, so the mix baked into each MIDI file is preserved.
     */
    static SysexMessage masterVolumeMessage(double volume) throws InvalidMidiDataException {
        // Gervill squares the master volume (0.5 -> 0.25 amplitude); the plugin's volume is already the
        // target amplitude, so send its square root to get that level out.
        int v = (int) Math.round(Math.sqrt(Math.max(0, Math.min(1, volume))) * 0x3FFF);
        byte[] data = {(byte) 0xF0, 0x7F, 0x7F, 0x04, 0x01, (byte) (v & 0x7F), (byte) ((v >> 7) & 0x7F), (byte) 0xF7};
        return new SysexMessage(data, data.length);
    }

    private void applyVolume() {
        Synthesizer s = synthesizer;
        if (s == null || !s.isOpen()) return;
        double v = volume;
        if (v == appliedVolume) return;
        try {
            s.getReceiver().send(masterVolumeMessage(v), -1);
            appliedVolume = v;
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            log.warn("Could not set synthesizer volume", e);
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

    private void ensureSynthesizer(Filepath sbPath) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        if (synthesizer != null && synthesizer.isOpen() && sbPath.equals(loadedSoundbankPath)) return;
        closeSynthesizer();

        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();

        try (InputStream in = new BufferedInputStream(sbPath.openInputStream())) {
            soundbank = MidiSystem.getSoundbank(in);
        }
        synthesizer.loadAllInstruments(soundbank);
        loadedSoundbankPath = sbPath;
        applyVolume();
        log.info("Synthesizer opened with soundbank: {}", sbPath.getFileName());
    }

    private void playOnce(Request next) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        Sequence seq;
        try (InputStream in = new BufferedInputStream(next.midi().openInputStream())) {
            seq = MidiSystem.getSequence(in);
        }

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
                synchronized (wake) { try { wake.wait(50); } catch (InterruptedException e) { break; } }
            }
            log.debug("Stopped {} at {}ms of {}ms (closed={}, stillDesired={}, sequencerRunning={})", next.name(),
                sequencer.getMicrosecondPosition() / 1000, seq.getMicrosecondLength() / 1000,
                closed, next.equals(desired.get()), sequencer.isRunning());
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
            appliedVolume = -1;
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
