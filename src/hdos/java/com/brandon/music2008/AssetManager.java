package com.brandon.music2008;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import java.util.zip.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads and extracts the compact HDOS music assets on first use.
 * Assets land in ~/.runelite/music2008/assets/ so no path configuration is needed.
 */
final class AssetManager {
    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    // Replace with a real GitHub Release URL once assets are published.
    // The ZIP should contain cache/, music/, and fanfares/ at its root.
    static final String ASSETS_URL = "https://github.com/YOUR_REPO/releases/download/assets-v1/HDOS-Music-Assets-Compact.zip";

    static final Path ASSETS_DIR = Path.of(System.getProperty("user.home"), ".runelite", "music2008", "assets");

    static boolean isReady() {
        return Files.isDirectory(ASSETS_DIR.resolve("cache"))
            && Files.isDirectory(ASSETS_DIR.resolve("music"));
    }

    /**
     * Downloads and extracts assets if not already present.
     * Calls progress with status strings, throws on failure.
     */
    static void ensureAssets(Consumer<String> progress) throws Exception {
        if (isReady()) {
            log.info("Assets already present at {}", ASSETS_DIR);
            return;
        }
        Files.createDirectories(ASSETS_DIR);
        Path tmp = ASSETS_DIR.resolveSibling("assets-download.zip.tmp");
        progress.accept("Downloading 2008 music assets (~63 MB)…");
        log.info("Downloading assets from {}", ASSETS_URL);
        download(ASSETS_URL, tmp, progress);
        progress.accept("Extracting assets…");
        log.info("Extracting assets to {}", ASSETS_DIR);
        extract(tmp, ASSETS_DIR);
        Files.deleteIfExists(tmp);
        progress.accept("Assets ready");
        log.info("Assets installed to {}", ASSETS_DIR);
    }

    private static void download(String url, Path dest, Consumer<String> progress) throws Exception {
        URL u = URI.create(url).toURL();
        URLConnection conn = u.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        long total = conn.getContentLengthLong();
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(dest)) {
            byte[] buf = new byte[65536];
            long read = 0;
            int n;
            long lastReport = 0;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                read += n;
                long now = System.currentTimeMillis();
                if (now - lastReport > 500) {
                    lastReport = now;
                    if (total > 0) {
                        int pct = (int) (read * 100 / total);
                        progress.accept("Downloading assets " + pct + "% (" + (read / 1_048_576) + " / " + (total / 1_048_576) + " MB)");
                    } else {
                        progress.accept("Downloading assets " + (read / 1_048_576) + " MB…");
                    }
                }
            }
        }
    }

    private static void extract(Path zip, Path dest) throws Exception {
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path target = dest.resolve(entry.getName()).normalize();
                if (!target.startsWith(dest)) throw new IOException("ZIP path escape: " + entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (InputStream in = zf.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    /** Delete the assets directory so they will be re-downloaded on next use. */
    static void deleteAssets() throws IOException {
        if (!Files.exists(ASSETS_DIR)) return;
        Files.walkFileTree(ASSETS_DIR, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                Files.delete(f); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                if (e != null) throw e; Files.delete(d); return FileVisitResult.CONTINUE;
            }
        });
    }
}
