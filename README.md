# 2008 Music Soundtrack — RuneLite Plugin

Replaces OSRS music tracks with their 2008-era versions using native cache synthesis and the original instrument patches. When no 2008 match exists the original OSRS music plays unchanged.

## How it works

On first use the plugin downloads a ~63 MB compact asset package (original instrument patches + 826 music MIDIs + 396 fanfare MIDIs) and stores it in `~/.runelite/music2008/assets/`. No custom Java runtime or manual path configuration is required.

Track matching uses cache name hashes against the displayed song name. 481 of 899 OSRS music archives have a confirmed unique 2008 match. Unmatched tracks play original OSRS music.

## Install

1. Build the fat JAR: `.\gradlew.bat --no-daemon test jar`
2. Copy `build/libs/2008-music-soundtrack-0.1.0-local.jar` to `~/.runelite/sideloaded-plugins/`
3. Launch RuneLite with `--developer-mode`
4. Enable **2008 Music Soundtrack** in the plugin list

Assets download automatically on first enable. Progress is shown in the plugin's sidebar panel.

## Development status

This is a working prototype, not a Plugin Hub release. Known limitations:

- 418 OSRS archives have no verified 2008 match and retain original music
- Jingle/fanfare replacement is scaffolded but not yet wired (jingle script ID not yet confirmed from a live session)
- Lifecycle and restoration behavior not fully verified across all transition types

## Repository layout

| Path | Purpose |
|---|---|
| `src/main/java/` | Plugin source |
| `src/main/resources/` | Plugin properties, music index TSV |
| `src/test/java/` | Tests |
| `libs/` | Synthesis and cache JARs |
| `packaging/` | Asset assembly scripts |
