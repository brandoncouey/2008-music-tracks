package com.brandon.music2008;

import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.*;
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

@PluginDescriptor(name="2008 Music Soundtrack", description="Play mapped HDOS music with its cache instruments; retain original music when unavailable", tags={"music","hdos","sound","2008"}, enabledByDefault=true)
public class Music2008Plugin extends Plugin {
    private static final Logger log=LoggerFactory.getLogger(Music2008Plugin.class);
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private Music2008Config config;
    @Inject private ClientToolbar toolbar;
    private PlaybackEngine engine;
    private TrackCatalog catalog;
    private NavigationButton navigation;
    private JLabel statusLabel;
    private String catalogueError;
    private volatile String lastName;
    private long revision;
    private boolean mutedByUs;
    private volatile String preview;
    private volatile boolean reload;
    private long lastUiUpdate;

    @Provides Music2008Config provideConfig(ConfigManager manager) { return manager.getConfig(Music2008Config.class); }
    @Override protected void startUp() {
        engine=new PlaybackEngine();
        SwingUtilities.invokeLater(this::addPanel);
        log.info("2008 Music Soundtrack enabled; native cache synthesis; cache={}",config.cacheFolder());
        Thread assetThread=new Thread(()->{
            try {
                AssetManager.ensureAssets(msg->SwingUtilities.invokeLater(()->{if(statusLabel!=null)statusLabel.setText("<html>"+escape(msg)+"</html>");}));
            } catch (Exception e) {
                log.warn("Asset download failed; configure cache/MIDI paths manually",e);
                SwingUtilities.invokeLater(()->{if(statusLabel!=null)statusLabel.setText("<html>Asset download failed: "+escape(e.getMessage())+"<br>See plugin settings to configure paths manually.</html>");});
            }
            reloadCatalogue();
        },"music2008-assets");
        assetThread.setDaemon(true);
        assetThread.start();
    }
    private void reloadCatalogue() {
        try { catalog=TrackCatalog.load(Path.of(config.midiFolder()));catalogueError=null; }
        catch (Exception e) { catalog=null;catalogueError=e.getMessage();log.warn("Cannot load 2008 Music Soundtrack catalogue",e); }
        lastName=null;revision++;reload=false;
    }
    private void addPanel() {
        if (engine==null) return;
        PluginPanel panel=new PluginPanel() {}; panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        panel.add(new JLabel("2008 Music Soundtrack — local test build"));
        panel.add(Box.createVerticalStrut(12));
        statusLabel=new JLabel("Waiting for music");panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(12));
        JTextField name=new JTextField("Scape Main");name.setMaximumSize(new Dimension(225,30));panel.add(name);
        JButton play=new JButton("Preview 2008 track");play.addActionListener(e->preview=name.getText().trim());panel.add(play);
        JButton stop=new JButton("Return to game music");stop.addActionListener(e->{preview=null;lastName=null;});panel.add(stop);
        JButton refresh=new JButton("Reload files / retry");refresh.addActionListener(e->{reload=true;preview=null;});panel.add(refresh);
        JButton redownload=new JButton("Re-download assets");redownload.addActionListener(e->{
            redownload.setEnabled(false);
            Thread t=new Thread(()->{
                try {
                    AssetManager.deleteAssets();
                    AssetManager.ensureAssets(msg->SwingUtilities.invokeLater(()->{if(statusLabel!=null)statusLabel.setText("<html>"+escape(msg)+"</html>");}));
                } catch(Exception ex) { log.warn("Re-download failed",ex); }
                reload=true;
                SwingUtilities.invokeLater(()->redownload.setEnabled(true));
            },"music2008-redownload");
            t.setDaemon(true);t.start();
        });panel.add(redownload);
        panel.add(Box.createVerticalStrut(12));
        panel.add(new JLabel("<html>Automatic matches use cache name hashes.<br>Unmatched songs retain OSRS music.<br>Configure cache/MIDI paths in plugin settings.<br>Disable Music Replacer during testing.</html>"));
        BufferedImage icon=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=icon.createGraphics();g.setColor(new Color(92,185,220));g.fillOval(2,2,28,28);g.setColor(Color.BLACK);g.setFont(new Font("SansSerif",Font.BOLD,18));g.drawString("H",9,23);g.dispose();
        navigation=NavigationButton.builder().tooltip("2008 Music Soundtrack").icon(icon).priority(8).panel(panel).build();toolbar.addNavigation(navigation);
    }
    @Subscribe public void onConfigChanged(ConfigChanged event) { if(Music2008Config.GROUP.equals(event.getGroup())) reload=true; }
    @Subscribe public void onGameStateChanged(GameStateChanged event) {
        if(event.getGameState()==GameState.LOGIN_SCREEN || event.getGameState()==GameState.CONNECTION_LOST) {
            preview=null;lastName=null;if(engine!=null)engine.request(null);restoreOriginal();
        }
    }
    @Subscribe public void onClientTick(ClientTick event) {
        if(engine==null)return;
        if(reload) { engine.request(null);restoreOriginal();reloadCatalogue(); }
        String name=preview;
        if(name==null && client.getGameState()==GameState.LOGGED_IN) {
            Widget now=client.getWidget(InterfaceID.Music.NOW_PLAYING_TEXT);
            if(now!=null)name=now.getText();
        }
        if(name!=null)name=name.replaceAll("<[^>]*>","").trim();
        Path midi=name==null || catalog==null ? null : catalog.midiFor(name);
        PlaybackEngine.Request request=midi==null ? null : new PlaybackEngine.Request(name,Path.of(config.cacheFolder()),midi,config.loop(),revision);
        if(!Objects.equals(name,lastName)) {
            if(config.diagnostics())log.info("2008 Music detected track={} archive={} replacement={}",name,catalog==null||name==null?null:catalog.archiveFor(name),midi);
            lastName=name;
        }
        engine.request(request);
        double volume=effectiveVolume();
        if(preview!=null && client.getGameState()!=GameState.LOGGED_IN)volume=0.35;
        engine.setVolume(volume);
        if(engine.isActive(request)) { client.setMusicVolume(0);mutedByUs=true; }
        else restoreOriginal();
        long now=System.currentTimeMillis();
        if(now-lastUiUpdate>250) {
            String message=catalogueError!=null?"Check settings: "+catalogueError:midi==null?"Original OSRS music: "+(name==null?"waiting for music tab":name):engine.status();
            String info=message+" | "+(catalog==null?0:catalog.available())+" MIDI files";
            SwingUtilities.invokeLater(()->{if(statusLabel!=null)statusLabel.setText("<html>"+escape(info)+"</html>");});lastUiUpdate=now;
        }
    }
    private double effectiveVolume() {
        double master=Math.max(0,Math.min(100,client.getVarpValue(VarPlayerID.OPTION_MASTER_VOLUME)))/100d;
        double music=Math.max(0,Math.min(100,client.getVarpValue(VarPlayerID.OPTION_MUSIC)))/100d;
        return master*master*music*music;
    }
    private void restoreOriginal() {
        if(!mutedByUs)return;
        mutedByUs=false;
        int volume=client.getVarpValue(VarPlayerID.OPTION_MUSIC);
        client.setMusicVolume((int)Math.round(Math.max(0,Math.min(100,volume))*2.55));
    }
    private static String escape(String text) { return text.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    @Override protected void shutDown() {
        PlaybackEngine previous=engine;engine=null;if(previous!=null)previous.close();preview=null;
        clientThread.invoke(this::restoreOriginal);
        SwingUtilities.invokeLater(()->{if(navigation!=null){toolbar.removeNavigation(navigation);navigation=null;}statusLabel=null;});
        log.info("2008 Music Soundtrack disabled; original music restored");
    }
}
