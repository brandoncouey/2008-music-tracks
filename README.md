# 2008 Music Soundtrack — RuneLite Plugin

Replaces OSRS music tracks with their 2008-era versions using the original RuneScape instrument sounds. When no 2008 match exists, the original OSRS music plays unchanged.

## How it works

On first use the plugin downloads a ~111 MB asset package (SF2 soundfont + 826 music MIDIs + 396 fanfare MIDIs) to `~/.runelite/music2008/assets/`. No manual configuration is needed.

Track matching uses name hashing against the displayed song name. 481 of 899 OSRS music archives have a confirmed unique 2008 match. Unmatched tracks play original OSRS music.

## Install

Available on the RuneLite [Plugin Hub](https://runelite.net/plugin-hub). Search for **2008 Music Soundtrack**.

## Configuration

| Option | Default | Description |
|--------|---------|-------------|
| Music files folder | Auto | Override the MIDI folder path |
| Fanfare files folder | Auto | Override the fanfare MIDI folder |
| Loop replacements | On | Repeat while OSRS keeps the same song |
| Enable jingle replacement | Off | Experimental fanfare replacement |
| Log track changes | On | Writes match results to RuneLite log |

## Repository layout

| Path | Purpose |
|------|---------|
| `src/main/java/` | Plugin source |
| `src/main/resources/` | Plugin properties, track index TSV |
| `src/test/java/` | Tests |
| `packaging/` | One-time asset generation tools |
