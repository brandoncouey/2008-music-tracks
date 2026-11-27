package com.brandon.music2008;

import main.HdosSynth;
import javax.sound.sampled.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** All native synth access stays on one thread: upstream decoding has static state. */
final class PlaybackEngine implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(PlaybackEngine.class);
    private static final Object SYNTH_LOCK = new Object();

    record Request(String name, Path cache, Path midi, boolean loop, long revision) {}

    private final Object wake = new Object();
    private final AtomicReference<Request> desired = new AtomicReference<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "music2008-audio"); t.setDaemon(true); return t;
    });

    private volatile Request active;
    private volatile String status = "Waiting for a song";
    private volatile double volume = 1.0;
    private volatile boolean closed;
    private volatile SourceDataLine output;

    PlaybackEngine() { worker.submit(this::run); }

    void request(Request request) {
        desired.set(request);
        synchronized (wake) { wake.notifyAll(); }
    }

    void setVolume(double value) { volume = Math.max(0, Math.min(1, value)); }
    boolean isActive(Request request) { return request != null && request.equals(active); }
    String status() { return status; }

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
            log.info("Loading {} from {}", next.name(), next.midi());
            try {
                synchronized (SYNTH_LOCK) {
                    if (closed || !next.equals(desired.get())) continue;
                    do {
                        try (HdosSynth synth = new HdosSynth(next.cache(), Files.readAllBytes(next.midi()))) {
                            AudioFormat fmt = new AudioFormat(44100, 16, 2, true, false);
                            try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                                output = line;
                                line.open(fmt, 8192);
                                byte[] pcm = new byte[1024];
                                synth.read(pcm, volume);
                                if (closed || !next.equals(desired.get())) break;
                                line.start();
                                active = next;
                                status = "Playing: " + next.name();
                                log.info("Playing {}", next.name());
                                while (!closed && next.equals(desired.get()) && !synth.isFinished()) {
                                    line.write(pcm, 0, pcm.length);
                                    synth.read(pcm, volume);
                                }
                                if (next.equals(desired.get()) && !closed) line.drain();
                                line.stop(); line.flush();
                            } finally { active = null; output = null; }
                        }
                    } while (!closed && next.loop() && next.equals(desired.get()));
                }
                if (!closed) status = "Waiting for the next song";
            } catch (Exception e) {
                active = null;
                status = "Unavailable; original music retained: " + e.getClass().getSimpleName();
                log.warn("Playback failed for {}; retaining original music", next.name(), e);
            }
        }
        active = null;
    }

    @Override public void close() {
        closed = true;
        desired.set(null);
        synchronized (wake) { wake.notifyAll(); }
        SourceDataLine line = output;
        if (line != null) line.close();
        worker.shutdownNow();
    }
}
