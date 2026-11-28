package com.brandon.music2008;

import java.io.*;
import java.util.function.Consumer;
import java.util.zip.*;
import net.runelite.client.util.Filepath;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads and extracts the compact 2008 music assets on first use.
 * Assets land in the plugin's data directory so no path configuration is needed.
 */
final class AssetManager {
    private static final Logger log = LoggerFactory.getLogger(AssetManager.class);

    static final String ASSETS_URL = "https://github.com/brandoncouey/2008-music-tracks/releases/download/assets-v3/2008-music-assets-v3.zip";

    static Filepath assetsDir;

    static void init(Filepath pluginDir) {
        assetsDir = pluginDir.join("assets");
    }

    static Filepath soundbankPath() {
        return assetsDir.join("soundbank.sf2");
    }

    static boolean isReady() {
        return soundbankPath().isFile()
            && assetsDir.join("music").isDirectory();
    }

    /**
     * Downloads and extracts assets if not already present.
     * Calls progress with status strings, throws on failure.
     */
    static void ensureAssets(OkHttpClient http, Consumer<String> progress) throws Exception {
        if (isReady()) {
            log.info("Assets already present at {}", assetsDir);
            return;
        }
        assetsDir.createDirectories();
        Filepath tmp = assetsDir.getParent().join("assets-download.zip.tmp");
        progress.accept("Downloading 2008 music assets (~100 MB)…");
        log.info("Downloading assets from {}", ASSETS_URL);
        download(http, ASSETS_URL, tmp, progress);
        progress.accept("Extracting assets…");
        log.info("Extracting assets to {}", assetsDir);
        extract(tmp, assetsDir);
        tmp.deleteIfExists();
        progress.accept("Assets ready");
        log.info("Assets installed to {}", assetsDir);
    }

    private static void download(OkHttpClient http, String url, Filepath dest, Consumer<String> progress) throws Exception {
        Request request = new Request.Builder().url(url).build();
        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code() + " from " + url);
            ResponseBody body = response.body();
            if (body == null) throw new IOException("Empty response body from " + url);
            long total = body.contentLength();
            try (InputStream in = body.byteStream();
                 OutputStream out = dest.openOutputStream()) {
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

    private static void extract(Filepath zip, Filepath dest) throws Exception {
        // ZipFile requires java.io.File — convert only at this javax.util.zip boundary
        try (ZipFile zf = new ZipFile(Filepath.Unchecked.getFile(zip))) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                // Filepath.join() enforces path containment, replacing the manual ZIP-slip check
                if (entry.isDirectory()) {
                    dest.join(entry.getName()).createDirectories();
                } else {
                    Filepath target = dest.join(entry.getName());
                    target.getParent().createDirectories();
                    try (InputStream in = zf.getInputStream(entry);
                         OutputStream out = target.openOutputStream()) {
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
        if (assetsDir.exists()) assetsDir.deleteRecursively();
    }
}
