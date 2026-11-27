package com.brandon.music2008;

import net.runelite.client.config.*;
import java.nio.file.Path;

@ConfigGroup(Music2008Config.GROUP)
public interface Music2008Config extends Config {
    String GROUP = "music2008";
    @ConfigItem(keyName="cacheFolder", name="HDOS cache (optional)", description="Override the auto-managed cache folder. Leave blank to use ~/.runelite/music2008/assets/cache", position=1)
    default String cacheFolder() {
        String bundle=System.getProperty("music2008.bundle");
        if (bundle != null) return Path.of(bundle,"assets","cache").toString();
        return AssetManager.ASSETS_DIR.resolve("cache").toString();
    }
    @ConfigItem(keyName="midiFolder", name="HDOS MIDI folder (optional)", description="Override the auto-managed MIDI folder. Leave blank to use ~/.runelite/music2008/assets/music", position=2)
    default String midiFolder() {
        String bundle=System.getProperty("music2008.bundle");
        if (bundle != null) return Path.of(bundle,"assets","music").toString();
        return AssetManager.ASSETS_DIR.resolve("music").toString();
    }
    @ConfigItem(keyName="fanfareFolder", name="HDOS fanfare folder (optional)", description="Override the auto-managed fanfare folder. Leave blank to use ~/.runelite/music2008/assets/fanfares", position=3)
    default String fanfareFolder() {
        String bundle=System.getProperty("music2008.bundle");
        if (bundle != null) return Path.of(bundle,"assets","fanfares").toString();
        return AssetManager.ASSETS_DIR.resolve("fanfares").toString();
    }
    @ConfigItem(keyName="loop", name="Loop replacements", description="Repeat the HDOS song while OSRS keeps the same song selected", position=4)
    default boolean loop() { return true; }
    @ConfigItem(keyName="fanfares", name="Enable jingle replacement (experimental)", description="Replace OSRS fanfares/jingles with HDOS versions. Requires a live session to confirm script IDs.", position=5)
    default boolean fanfares() { return false; }
    @ConfigItem(keyName="diagnostics", name="Log track changes + script IDs", description="Write detected names, mapping results, script IDs, and playback failures to the RuneLite log. Enable to help identify jingle script IDs.", position=6)
    default boolean diagnostics() { return true; }
}
