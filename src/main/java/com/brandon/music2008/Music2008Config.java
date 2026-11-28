package com.brandon.music2008;

import net.runelite.client.config.*;

@ConfigGroup(Music2008Config.GROUP)
public interface Music2008Config extends Config {
    String GROUP = "music2008";

    @ConfigItem(keyName="midiFolder", name="Music files folder (optional)", description="Override the auto-managed MIDI folder. Leave blank to use the plugin data directory.", position=1)
    default String midiFolder() {
        return "";
    }

    @ConfigItem(keyName="fanfareFolder", name="Fanfare files folder (optional)", description="Override the auto-managed fanfare folder. Leave blank to use the plugin data directory.", position=2)
    default String fanfareFolder() {
        return "";
    }

    @ConfigItem(keyName="loop", name="Loop replacements", description="Repeat the song while OSRS keeps the same song selected", position=3)
    default boolean loop() { return true; }

    @ConfigItem(keyName="fanfares", name="Enable jingle replacement (experimental)", description="Replace OSRS fanfares/jingles with 2008 versions. Requires a live session to confirm script IDs.", position=4)
    default boolean fanfares() { return false; }

    @ConfigItem(keyName="diagnostics", name="Log track changes + script IDs", description="Write detected names, mapping results, script IDs, and playback failures to the RuneLite log. Enable to help identify jingle script IDs.", position=5)
    default boolean diagnostics() { return true; }
}
