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
    @ConfigItem(keyName="loop", name="Loop replacements", description="Repeat the HDOS song while OSRS keeps the same song selected", position=3)
    default boolean loop() { return true; }
    @ConfigItem(keyName="diagnostics", name="Log track changes", description="Write detected names, mapping results, and playback failures to the RuneLite log", position=4)
    default boolean diagnostics() { return true; }
}
