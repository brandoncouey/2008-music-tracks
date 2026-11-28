package com.brandon.music2008;

import java.io.*;
import java.util.function.Consumer;
import java.util.zip.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads and extracts the compact 2008 music assets on first use.
 * Assets land in ~/.runelite/music2008/assets/ so no path configuration is needed.
 */
final class AssetManager {
    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    // Replace with a real GitHub Release URL once assets are published.
    // The ZIP should contain cache/, music/, and fanfares/ at its root.
    static final String ASSETS_URL = "https://github.com/brandoncouey/2008-music-tracks/releases/download/assets-v3/2008-music-assets-v3.zip";

    static final File ASSETS_DIR = new File(new File(new File(System.getProperty("user.home"), ".runelite"), "music2008"), "assets");

    static File soundbankPath() {
        return new File(ASSETS_DIR, "soundbank.sf2");
    }

    static boolean isReady() {
        return soundbankPath().isFile()
            && new File(ASSETS_DIR, "music").isDirectory();
    }

    /**
     * Downloads and extracts assets if not already present.
     * Calls progress with status strings, throws on failure.
     */
    static void ensureAssets(OkHttpClient http, Consumer<String> progress) throws Exception {
        if (isReady()) {
            log.info("Assets already present at {}", ASSETS_DIR);
            return;
        }
        ASSETS_DIR.mkdirs();
        File tmp = new File(ASSETS_DIR.getParentFile(), "assets-download.zip.tmp");
        progress.accept("Downloading 2008 music assets (~100 MB)…");
        log.info("Downloading assets from {}", ASSETS_URL);
        download(http, ASSETS_URL, tmp, progress);
        progress.accept("Extracting assets…");
        log.info("Extracting assets to {}", ASSETS_DIR);
        extract(tmp, ASSETS_DIR);
        tmp.delete();
        progress.accept("Assets ready");
        log.info("Assets installed to {}", ASSETS_DIR);
    }

    private static void download(OkHttpClient http, String url, File dest, Consumer<String> progress) throws Exception {
        Request request = new Request.Builder().url(url).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " from " + url);
            ResponseBody body = response.body();
            if (body == null) throw new IOException("Empty response body from " + url);
            long total = body.contentLength();
            try (InputStream in = body.byteStream();
                 OutputStream out = new FileOutputStream(dest)) {
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
    }

    private static void extract(File zip, File dest) throws Exception {
        String destCanon = dest.getCanonicalPath();
        try (ZipFile zf = new ZipFile(zip)) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File target = new File(dest, entry.getName()).getCanonicalFile();
                String targetCanon = target.getCanonicalPath();
                if (!targetCanon.equals(destCanon) && !targetCanon.startsWith(destCanon + File.separator))
                    throw new IOException("ZIP path escape: " + entry.getName());
                if (entry.isDirectory()) {
                    target.mkdirs();
                } else {
                    target.getParentFile().mkdirs();
                    try (InputStream in = zf.getInputStream(entry);
                         OutputStream out = new FileOutputStream(target)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                    }
                }
            }
        }
    }

    /** Delete the assets directory so they will be re-downloaded on next use. */
    static void deleteAssets() throws IOException {
        if (!ASSETS_DIR.exists()) return;
        deleteRecursive(ASSETS_DIR);
    }

    private static void deleteRecursive(File f) throws IOException {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        if (!f.delete()) throw new IOException("Cannot delete " + f);
    }
}
