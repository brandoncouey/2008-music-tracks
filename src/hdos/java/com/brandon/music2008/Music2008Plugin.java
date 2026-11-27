package com.brandon.music2008;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Objects;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name="2008 Music Soundtrack", description="Play HDOS music with its original instruments; retains OSRS music when unavailable", tags={"music","hdos","sound","2008"}, enabledByDefault=true)
public class Music2008Plugin extends Plugin {
    private static final Logger log = LoggerFactory.getLogger(Music2008Plugin.class);

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private Music2008Config config;
    @Inject private ClientToolbar toolbar;

    private PlaybackEngine engine;
    private TrackCatalog catalog;
    private NavigationButton navigation;

    // Panel widgets updated from background + client threads
    private volatile JLabel assetStatusLabel;
    private volatile JProgressBar downloadBar;
    private volatile JButton redownloadBtn;
    private volatile JLabel trackLabel;

    private String catalogueError;
    private volatile String lastName;
    private long revision;
    private boolean mutedByUs;
    private volatile String preview;
    private volatile boolean reload;
    private long lastUiUpdate;

    // Asset state
    private enum AssetState { CHECKING, DOWNLOADING, READY, FAILED }
    private volatile AssetState assetState = AssetState.CHECKING;
    private volatile String assetMessage = "Checking assets…";

    @Provides Music2008Config provideConfig(ConfigManager manager) {
        return manager.getConfig(Music2008Config.class);
    }

    @Override protected void startUp() {
        engine = new PlaybackEngine();
        SwingUtilities.invokeLater(this::buildPanel);
        log.info("2008 Music Soundtrack enabled; cache={}", config.cacheFolder());
        startAssetEnsure(false);
    }

    private void startAssetEnsure(boolean forceRedownload) {
        assetState = AssetState.CHECKING;
        assetMessage = forceRedownload ? "Deleting old assets…" : "Checking assets…";
        updateAssetUi();
        Thread t = new Thread(() -> {
            try {
                if (forceRedownload) AssetManager.deleteAssets();
                AssetManager.ensureAssets(this::onDownloadProgress);
                assetState = AssetState.READY;
                assetMessage = "Ready — assets installed";
            } catch (Exception e) {
                assetState = AssetState.FAILED;
                assetMessage = "Download failed: " + e.getMessage();
                log.warn("Asset download failed; configure cache/MIDI paths manually", e);
            }
            updateAssetUi();
            reloadCatalogue();
        }, "music2008-assets");
        t.setDaemon(true);
        t.start();
    }

    private void onDownloadProgress(String msg) {
        assetState = AssetState.DOWNLOADING;
        assetMessage = msg;
        // Parse percentage for the progress bar
        int pct = -1;
        int idx = msg.indexOf('%');
        if (idx > 0) {
            try { pct = Integer.parseInt(msg.substring(Math.max(0, idx - 3), idx).trim()); }
            catch (NumberFormatException ignored) {}
        }
        final int bar = pct;
        SwingUtilities.invokeLater(() -> {
            if (assetStatusLabel != null) assetStatusLabel.setText(msg);
            if (downloadBar != null) {
                downloadBar.setVisible(true);
                if (bar >= 0) { downloadBar.setIndeterminate(false); downloadBar.setValue(bar); }
                else downloadBar.setIndeterminate(true);
            }
        });
    }

    private void updateAssetUi() {
        SwingUtilities.invokeLater(() -> {
            if (assetStatusLabel != null) assetStatusLabel.setText(assetMessage);
            if (downloadBar != null) {
                boolean show = assetState == AssetState.DOWNLOADING || assetState == AssetState.CHECKING;
                downloadBar.setVisible(show);
                if (!show) downloadBar.setValue(0);
            }
            if (redownloadBtn != null) redownloadBtn.setEnabled(assetState != AssetState.DOWNLOADING && assetState != AssetState.CHECKING);
        });
    }

    private void reloadCatalogue() {
        try {
            catalog = TrackCatalog.load(Path.of(config.midiFolder()));
            catalogueError = null;
            log.info("Loaded {} MIDI files", catalog.available());
        } catch (Exception e) {
            catalog = null;
            catalogueError = e.getMessage();
            log.warn("Cannot load catalogue", e);
        }
        lastName = null;
        revision++;
        reload = false;
    }

    private void buildPanel() {
        if (engine == null) return;
        PluginPanel panel = new PluginPanel() {};
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header
        JLabel header = new JLabel("2008 Music Soundtrack");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(header);
        panel.add(Box.createVerticalStrut(14));

        // ── Assets section ──
        JLabel assetsTitle = bold("ASSETS");
        assetsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(assetsTitle);
        panel.add(Box.createVerticalStrut(4));

        assetStatusLabel = new JLabel(assetMessage);
        assetStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(assetStatusLabel);

        downloadBar = new JProgressBar(0, 100);
        downloadBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        downloadBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        downloadBar.setIndeterminate(true);
        downloadBar.setVisible(assetState == AssetState.DOWNLOADING || assetState == AssetState.CHECKING);
        panel.add(Box.createVerticalStrut(4));
        panel.add(downloadBar);
        panel.add(Box.createVerticalStrut(6));

        redownloadBtn = new JButton("Re-download assets");
        redownloadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        redownloadBtn.setEnabled(assetState != AssetState.DOWNLOADING && assetState != AssetState.CHECKING);
        redownloadBtn.addActionListener(e -> startAssetEnsure(true));
        panel.add(redownloadBtn);
        panel.add(Box.createVerticalStrut(14));

        // ── Now Playing section ──
        JLabel trackTitle = bold("NOW PLAYING");
        trackTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(trackTitle);
        panel.add(Box.createVerticalStrut(4));

        trackLabel = new JLabel("Waiting for music tab…");
        trackLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(trackLabel);
        panel.add(Box.createVerticalStrut(14));

        // ── Preview section ──
        JLabel previewTitle = bold("PREVIEW");
        previewTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(previewTitle);
        panel.add(Box.createVerticalStrut(4));

        JTextField nameField = new JTextField("Scape Main");
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        nameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(4));

        JPanel previewRow = new JPanel(new GridLayout(1, 2, 4, 0));
        previewRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JButton playBtn = new JButton("▶ Play");
        playBtn.addActionListener(e -> preview = nameField.getText().trim());
        JButton stopBtn = new JButton("■ Stop");
        stopBtn.addActionListener(e -> { preview = null; lastName = null; });
        previewRow.add(playBtn); previewRow.add(stopBtn);
        panel.add(previewRow);
        panel.add(Box.createVerticalStrut(14));

        // ── Controls section ──
        JButton reloadBtn = new JButton("Reload files / retry");
        reloadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        reloadBtn.addActionListener(e -> { reload = true; preview = null; });
        panel.add(reloadBtn);
        panel.add(Box.createVerticalStrut(14));

        // ── Help text ──
        JLabel help = new JLabel("<html><small>Automatic matches use cache name hashes.<br>"
            + "Unmatched songs play original OSRS music.<br>"
            + "Override cache/MIDI paths in plugin settings.</small></html>");
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(help);

        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setColor(new Color(92, 185, 220)); g.fillOval(2, 2, 28, 28);
        g.setColor(Color.BLACK); g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.drawString("H", 9, 23);
        g.dispose();
        navigation = NavigationButton.builder().tooltip("2008 Music Soundtrack").icon(icon).priority(8).panel(panel).build();
        toolbar.addNavigation(navigation);
    }

    private static JLabel bold(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    @Subscribe public void onConfigChanged(ConfigChanged event) {
        if (Music2008Config.GROUP.equals(event.getGroup())) reload = true;
    }

    // VarbitChanged covers both varbit and varp changes in this RuneLite version.
    // MUSIC_CURRENT_ID fires when the game switches to a new song — reset lastName
    // so the next ClientTick re-resolves immediately without waiting for a widget update.
    @Subscribe public void onVarbitChanged(VarbitChanged event) {
        int id = event.getVarpId();
        if (id == VarPlayerID.MUSIC_CURRENT_ID || id == VarPlayerID.MUSIC_CURRENT_TRACK) {
            if (config.diagnostics())
                log.info("2008 Music varp {} → {}", id, event.getValue());
            lastName = null;
        }
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        if (state == GameState.LOGIN_SCREEN || state == GameState.CONNECTION_LOST) {
            preview = null; lastName = null;
            if (engine != null) engine.request(null);
            restoreOriginal();
        }
    }

    @Subscribe public void onClientTick(ClientTick event) {
        if (engine == null) return;
        if (reload) { engine.request(null); restoreOriginal(); reloadCatalogue(); }

        String name = preview;
        if (name == null && client.getGameState() == GameState.LOGGED_IN) {
            Widget now = client.getWidget(InterfaceID.Music.NOW_PLAYING_TEXT);
            if (now != null) name = now.getText();
        }
        if (name != null) name = name.replaceAll("<[^>]*>", "").trim();

        // Don't activate replacement when game music is muted
        double vol = effectiveVolume();
        boolean gameMuted = vol < 0.001;
        if (preview != null && client.getGameState() != GameState.LOGGED_IN) vol = 0.35;

        Path midi = (name == null || catalog == null || gameMuted) ? null : catalog.midiFor(name);
        PlaybackEngine.Request request = midi == null ? null
            : new PlaybackEngine.Request(name, Path.of(config.cacheFolder()), midi, config.loop(), revision);

        if (!Objects.equals(name, lastName)) {
            if (config.diagnostics()) log.info("2008 Music track={} osrsId={} hdosArchive={} replacement={}",
                name,
                client.getVarpValue(VarPlayerID.MUSIC_CURRENT_ID),
                catalog == null || name == null ? null : catalog.archiveFor(name),
                midi);
            lastName = name;
        }

        engine.request(request);
        engine.setVolume(vol);

        if (engine.isActive(request)) {
            // Use 1 (not 0) so OSRS keeps the audio stream alive — when we restore,
            // the song resumes immediately without needing a restart.
            client.setMusicVolume(1);
            mutedByUs = true;
        } else {
            restoreOriginal();
        }

        long now = System.currentTimeMillis();
        if (now - lastUiUpdate > 250) {
            lastUiUpdate = now;
            String trackText;
            if (catalogueError != null) {
                trackText = "<font color='red'>Settings error: " + escape(catalogueError) + "</font>";
            } else if (name == null) {
                trackText = "<i>Waiting for music tab…</i>";
            } else if (midi == null) {
                trackText = escape(name) + " <font color='gray'>(no HDOS match)</font>";
            } else {
                trackText = escape(name) + " <font color='green'>→ HDOS</font><br><small>" + escape(engine.status()) + "</small>";
            }
            int available = catalog == null ? 0 : catalog.available();
            final String tt = trackText;
            final String am = assetState == AssetState.READY
                ? "Ready — " + available + " tracks" : assetMessage;
            SwingUtilities.invokeLater(() -> {
                if (trackLabel != null) trackLabel.setText("<html>" + tt + "</html>");
                if (assetState == AssetState.READY && assetStatusLabel != null)
                    assetStatusLabel.setText(am);
            });
        }
    }

    private double effectiveVolume() {
        double master = Math.max(0, Math.min(100, client.getVarpValue(VarPlayerID.OPTION_MASTER_VOLUME))) / 100d;
        double music  = Math.max(0, Math.min(100, client.getVarpValue(VarPlayerID.OPTION_MUSIC))) / 100d;
        return master * master * music * music;
    }

    private void restoreOriginal() {
        if (!mutedByUs) return;
        mutedByUs = false;
        int raw = client.getVarpValue(VarPlayerID.OPTION_MUSIC);
        client.setMusicVolume((int) Math.round(Math.max(0, Math.min(100, raw)) * 2.55));
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override protected void shutDown() {
        PlaybackEngine prev = engine; engine = null;
        if (prev != null) prev.close();
        preview = null;
        clientThread.invoke(this::restoreOriginal);
        SwingUtilities.invokeLater(() -> {
            if (navigation != null) { toolbar.removeNavigation(navigation); navigation = null; }
            assetStatusLabel = null; downloadBar = null; redownloadBtn = null; trackLabel = null;
        });
        log.info("2008 Music Soundtrack disabled; original music restored");
    }
}
