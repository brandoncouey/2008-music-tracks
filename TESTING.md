# 2008 Music Soundtrack — Tester Guide

Thanks for helping test! This guide gets you from zero to hearing HDOS music in about 5 minutes.

## What this is

A RuneLite plugin that replaces OSRS music with the 2008 HDOS arrangements, using the original instrument patches so it sounds authentic rather than a MIDI soundfont. When a song has no HDOS version your original OSRS music plays normally.

Currently **481 of 899** OSRS tracks have confirmed HDOS matches. You'll hear the original OSRS music for the other 418.

---

## Requirements

- Windows 10 or 11 (64-bit)
- [RuneLite](https://runelite.net) already installed
- An OSRS account (your own — the plugin doesn't touch logins)
- Internet access for the one-time ~63 MB asset download

---

## Install (one time)

1. **Download** [`Install-Tester.ps1`](https://github.com/brandoncouey/hdos-music-plugin/releases/latest/download/Install-Tester.ps1) from the releases page
2. **Right-click** it → **Run with PowerShell**
   - If Windows warns about an unsigned script, click **More info → Run anyway**
3. The script places the plugin JAR and creates a desktop shortcut called **RuneLite — 2008 Music Test**

That's it.

---

## First launch

1. Double-click **RuneLite — 2008 Music Test** on your desktop
2. Log in with your normal account
3. The plugin auto-enables. Watch the **H sidebar panel** (blue H icon on the right toolbar)
4. It will show **"Downloading assets…"** with a progress bar — this is the ~63 MB one-time download
5. Once it shows **"Ready — 826 tracks, 396 fanfares"** you're set

---

## Using the plugin

- **Walk to a new area** — music switches automatically when OSRS changes the track
- **Open the Music tab** — the plugin picks up the currently playing song name
- The **H panel** shows what's playing and whether an HDOS match was found
- If the sidebar shows `(no HDOS match)` in gray that area uses original OSRS music — that's expected

### Volume

The plugin follows your in-game music and master volume sliders. Muting the music slider pauses replacement too.

### Preview

Type a song name in the H panel and click **▶ Play** to test a specific track without walking anywhere.

---

## What to test and report

Please note which of these work and which don't:

- [ ] Music plays when you first log in
- [ ] Music changes automatically when you walk to a new area
- [ ] Volume slider in OSRS settings controls the replacement volume
- [ ] Muting OSRS music (slider to 0) stops the replacement
- [ ] Un-muting resumes seamlessly without needing to toggle
- [ ] Disabling and re-enabling the plugin from Plugin Hub works cleanly
- [ ] Logging out and back in works (music resumes)
- [ ] The H panel shows the current track name correctly
- [ ] Preview function plays the track you typed

**Please report:** song name, what you expected, what actually happened.

---

## Known limitations (don't report these)

- 418 tracks have no HDOS version and play original OSRS music — this is intentional
- Jingle/fanfare replacement (level-up chime, quest complete) is not yet implemented
- Very first login after install may take a few seconds while assets download in the background

---

## Uninstall

Run `Uninstall-Tester.ps1` (same way as the installer). Your RuneLite account and settings are untouched.

---

## Report issues

[github.com/brandoncouey/hdos-music-plugin/issues](https://github.com/brandoncouey/hdos-music-plugin/issues)

Include: Windows version, what song was playing, what the H panel showed, and the RuneLite log (`%USERPROFILE%\.runelite\logs\client.log`).
