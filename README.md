# 2008 Music Soundtrack — RuneLite Plugin

Replaces OSRS music tracks with their HDOS (2008-era) versions using native cache synthesis and the original HDOS instrument patches. When no HDOS match exists the original OSRS music plays unchanged.

## How it works

On first use the plugin downloads a ~63 MB compact asset package (HDOS instrument patches + 826 music MIDIs + 396 fanfare MIDIs) and stores it in `~/.runelite/music2008/assets/`. No HDOS installation, custom Java runtime, or manual path configuration is required.

Track matching uses HDOS cache name hashes against the displayed song name. 481 of 899 OSRS music archives have a confirmed unique HDOS match. Unmatched tracks play original OSRS music.

## Install

1. Build the fat JAR: `.\gradlew.bat --no-daemon test jar`
2. Copy `build/libs/2008-music-soundtrack-0.1.0-local.jar` to `~/.runelite/sideloaded-plugins/`
3. Launch RuneLite with `--developer-mode`
4. Enable **2008 Music Soundtrack** in the plugin list

Assets download automatically on first enable. Progress is shown in the plugin's sidebar panel.

## Development status

This is a working prototype, not a Plugin Hub release. Known limitations:

- 418 OSRS archives have no verified HDOS match and retain original music
- Jingle/fanfare replacement is scaffolded but not yet wired (jingle script ID not yet confirmed from a live session)
- Lifecycle and restoration behavior not fully verified across all transition types

## Repository layout

| Path | Purpose |
|---|---|
| `src/hdos/java/` | Active plugin source |
| `src/hdos/resources/` | Plugin properties, HDOS music index TSV |
| `src/hdosTest/java/` | Active tests |
| `src/main/` | Upstream Music Replacer source (reference only, excluded from build) |
| `libs/` | Native synthesis JARs (hdos-synth, Displee read-only, vorbis) |
| `packaging/` | Bundle assembly and verification scripts |

## Third-party notices

The native synthesiser is derived from [lequietriot/runescape-midi-player-suite](https://github.com/rune-status/lequietriot-runescape-midi-player-suite). HDOS instrument and MIDI assets are Jagex content. See `THIRD-PARTY-NOTICES.txt` for full details.

This project is not affiliated with Jagex or the RuneLite Plugin Hub. Do not publish or distribute without resolving applicable licensing and Plugin Hub review requirements.
