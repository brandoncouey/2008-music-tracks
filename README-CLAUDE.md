# HDOS Music Claude Handoff

This is the continuation README for Brandon's RuneLite HDOS music project. It describes the implemented prototype, verified artifacts, known failures, and the work needed to make installation automatic for ordinary players. Read the active source and current Git status before editing; this document records the handoff state rather than guaranteeing that a live process still uses the latest build.

The key recent result is that **players do not need the complete HDOS cache**. A compact package containing all extracted MIDI music and only the soundbank data has been built and verified. It is **63,114,162 bytes compressed**, approximately **63.1 MB / 60.2 MiB**. Prefer integrating that package before committing to a different playback architecture.

## Product goal

A player installs HDOS Music in their normal RuneLite client. When OSRS selects a song, the plugin plays the corresponding HDOS version using the correct instruments. The plugin follows area-driven and manual changes, volume, mute, looping, logout, and shutdown. Original OSRS music remains available when there is no verified or playable replacement.

The desired release experience is installation with no manual file selection, HDOS installation, MIDI dumping, custom Java installation, or alternate game launcher. First-use background downloading of a verified compact audio-assets package is a proposed implementation, **not an implemented feature**. No music hosting has been provisioned and no Plugin Hub submission has been made.

Automatic index 11 fanfare/jingle replacement is also a goal, but is not implemented.

## Locations and artifact status

| Item | Location |
| --- | --- |
| Active development project | `C:\Users\brrad\OneDrive\Desktop\HDOS Music Plugin` |
| This README | `README-CLAUDE.md` in the development project |
| Compact soundbank and MIDI folder | `C:\Users\brrad\OneDrive\Desktop\HDOS Music Assets - Compact` |
| Compact assets ZIP | `C:\Users\brrad\OneDrive\Desktop\HDOS-Music-Assets-Compact.zip` |
| Original portable bundle staging | `C:\Users\brrad\OneDrive\Desktop\HDOS Music - Windows` |
| Original full bundle ZIP | `C:\Users\brrad\OneDrive\Desktop\HDOS-Music-Windows-0.1.0.zip` |
| Full bundle checksum | Same path plus `.sha256` |
| Native synthesis source and validation | `C:\Users\brrad\hdos-render-work` |
| MIDI extraction and naming tools | `C:\Users\brrad\midi-dump-work` |
| Extracted MIDI collections | `C:\Users\brrad\OneDrive\Desktop\MIDI Dump` |
| Original HDOS cache | `C:\Users\brrad\hdos\bin\cache\staged\runescape` |
| Original RuneLite cache | `C:\Users\brrad\.runelite\jagexcache\oldschool\LIVE` |
| Music-tab screenshot | `C:\Users\brrad\OneDrive\Desktop\MusicListRunescape.png` |

The full bundle ZIP is **371,361,215 bytes**, approximately **371 MB / 354.2 MiB**. It includes the original large cache, Java runtime, and RuneLite client. **It has not been rebuilt around the compact soundbank.**

The compact ZIP is assets only. It does not contain an installer, Java, or RuneLite. It can be used by the current plugin by configuring its `cache` and `music` directories. Automatic detection/downloading of that ZIP remains work to do.

The screenshot contains a visible portion of the music list and indicates 688 entries. It is not a complete machine-readable list. Visible music-tab entries and raw cache archive counts are different quantities.

## Repository and build

The project was cloned from [Music Replacer](https://github.com/alowaniak/music-replacer). Original sources remain for provenance, but Gradle selects different source directories for the HDOS implementation.

Active paths:

```text
src/hdos/java/com/brandon/hdos/
src/hdos/resources/
src/hdosTest/java/com/brandon/hdos/
packaging/
libs/
```

The original `src/main` is excluded from the active source set. The existing root `README.md` and root `runelite-plugin.properties` still describe upstream Music Replacer. The actual active plugin metadata is `src/hdos/resources/runelite-plugin.properties`. Do not mistake the old documentation or old test launcher for the active implementation.

Build versions at handoff:

- Java 17 target.
- Gradle wrapper 9.3.1.
- RuneLite client/API 1.13.8.
- JUnit 4.13.2.
- Native dependencies packaged into the plugin JAR.

From the development project:

```powershell
.\gradlew.bat --no-daemon --console=plain test jar
```

Output:

```text
build\libs\hdos-music-0.1.0-local.jar
```

The previous build passed all five tests across `TrackCatalogTest`, `RestoreOriginalTest`, and `PortableConfigTest`. These are targeted tests, not proof of complete in-game behavior.

Work is uncommitted. Inspect `git status --short`; preserve unrelated changes, do not stage everything, and do not commit/push/publish without the applicable authorization and repository policy. The task creating this README did not rebuild or modify either ZIP.

## Current code architecture

| Class or resource | Responsibility |
| --- | --- |
| `HdosMusicPlugin` | RuneLite lifecycle, song detection, preview/status UI, native music muting and restoration |
| `TrackCatalog` | Cache-name-hash lookup and archive-ID MIDI file resolution |
| `HdosConfig` | Cache/MIDI paths, looping, diagnostics, portable bundle defaults |
| `PlaybackEngine` | Background synthesis and JavaSound PCM streaming |
| `hdos-music-index.tsv` | Bundled HDOS index 6 archive IDs and name hashes |

`HdosMusicPlugin` currently reads `InterfaceID.Music.NOW_PLAYING_TEXT` on `ClientTick`, strips markup, and resolves the displayed name. It has a blue H sidebar panel with named preview, return-to-game, reload/retry, and status controls.

`TrackCatalog` normalizes names and hashes signed Windows-1252 bytes with multiplication by 31. It rejects ambiguous hashes and resolves MIDI filenames by their leading archive ID. Check normalization against the actual cache format when investigating unmatched non-ASCII names; do not guess that an ID match means the same song.

`PlaybackEngine` uses one background worker and a static synthesis lock. It constructs a native synth, opens a `SourceDataLine`, and streams 44.1 kHz stereo signed 16-bit little-endian PCM. Native game music is muted only after the engine reports an active replacement. Construction/output errors leave the original music path available. Requests include a revision for reload/retry.

Upstream MIDI/Vorbis code has static mutable state. **Do not run multiple native synths concurrently**, including previews and validation jobs within the same JVM. The current transitions are basic; polished fades and complete lifecycle verification remain unfinished.

The implementation still uses worker `Thread.sleep()` polling and performs catalogue file scanning during startup/reload. Review these against current RuneLite plugin guidelines rather than assuming the prototype is submission-ready.

## Why MIDI alone is insufficient

A MIDI stores notes, timing, controllers, and instrument references. It does not contain the instrument recordings or synthesis rules needed to sound like HDOS.

The current native synthesizer uses these HDOS indexes:

| Index | Role |
| --- | --- |
| 4 | Synthesized sound effects and sample material |
| 14 | Vorbis sample data and decoder setup |
| 15 | Instrument patches |

This avoids replacing HDOS instruments with the Windows default MIDI soundfont. A separate SF2 export is not required for the compact package because the existing native synth already understands the preserved patch/sample bytes.

Native API:

```java
new main.HdosSynth(Path cacheFolder, byte[] midi);
int read(byte[] buffer, double volume);
boolean isFinished();
void close();
```

`read` expects at least 1,024 bytes and produces 256 stereo PCM frames per call at 44,100 Hz.

Runtime dependencies:

```text
libs/hdos-synth.jar
libs/Displee-RS-Cache-Library-readonly.jar
libs/vorbis-java-core-0.8.jar
```

The native source is based on [RuneScape MIDI Player Suite](https://github.com/rune-status/lequietriot-runescape-midi-player-suite), commit `bd31622893e10642dbaf699d4198b01ed7b7bbca`.

The cache library originally opened `RandomAccessFile` with `rw`. `read_only_cache.py` builds a separate derived JAR changing the exact mode constant to `r`. Use only the read-only JAR at runtime. The original writable JAR exists among native build inputs; it must not accidentally take precedence on the runtime classpath. Audit this approach before release.

Native instructions and build script:

```text
C:\Users\brrad\hdos-render-work\SYNTH-INTEGRATION.txt
C:\Users\brrad\hdos-render-work\build-synth.ps1
C:\Users\brrad\hdos-render-work\read_only_cache.py
```

## Compact soundbank breakthrough

`packaging/compact_soundbank.py` creates a new cache container containing sound data only. It does not edit the original cache or require changing the synth API.

The repack preserves original archive IDs and exact compressed archive bytes, rewrites sector chains, and retains index 255 reference metadata. Empty compatibility indexes remain because the cache reader expects the cache layout. It contains no map/model/texture payloads, though reference metadata for other indexes remains.

| Preserved data | Archive count |
| --- | ---: |
| Index 4 | 7,438 |
| Index 14 | 4,347 |
| Index 15 | 221 |
| Index 255 reference archives | 31 |
| Total verified preserved archives | 12,037 |

Sizes:

| Artifact | Exact bytes |
| --- | ---: |
| Original cache snapshot | 338,760,749 |
| Compact cache files, uncompressed | 55,674,022 |
| Compact ZIP including all music and fanfare MIDIs | 63,114,162 |

The compact folder contains:

```text
cache/
music/          826 MIDIs
fanfares/       396 MIDIs
verification.json
README.txt
verification-original.wav
verification-compact.wav
```

The two WAVs are comparison outputs in the staging folder and are excluded from the compact ZIP.

Verified specifically for the compact package:

1. All 12,037 preserved archive byte sequences match the original.
2. A three-second Autumn Voyage render from the original and compact caches produced byte-identical WAV files.
3. The WAV SHA256 is `f516dca6f7037f7f8674e77a3787fc83d5c289327c8add340b14aec61a2d7f9f`.
4. The compact ZIP passed CRC integrity checking.

The full 1,222-track instrument validation described below was run against the original cache. It has not been independently rerun against the compact container. Byte equality establishes that the preserved inputs match, but do not describe that as 1,222 separate compact-cache playback tests.

The script currently uses local source/destination assumptions and refuses to overwrite its existing output. Refactor it into a reproducible parameterized tool rather than deleting output casually to rerun it.

## Extraction and song mapping

The extraction decoder reference was [RS MIDI Dumper](https://github.com/Rune-Status/lequietriot-RS-MIDI-Dumper). Extraction used read-only sector access, reference checksums, and MIDI validation.

| Cache | Index 6 music | Index 11 fanfares |
| --- | ---: | ---: |
| HDOS | 826 | 396 |
| RuneLite | 899 | 316 |
| Older partial OSRS cache recovered | 3 | 314 |

All 2,754 extracted MIDI files parsed and had consistent chunk lengths. Main HDOS/RuneLite extraction was complete against each local reference table. The older cache was incomplete and is preserved separately.

Crosswalk results:

- 481 of 899 OSRS music archives have a unique HDOS name-hash match.
- 418 have no verified match.
- Of the 481 matched pairs, 399 have identical decoded MIDI bytes and 82 differ.

Those counts concern archives, not visible music-tab songs. A shared MIDI can sound different because of instruments. Conversely, an HDOS asset is not automatically a different arrangement just because it came from that client.

Reports:

```text
MIDI Dump/osrs-to-hdos-mapping.csv
MIDI Dump/track-names.csv
MIDI Dump/midi-manifest.csv
MIDI Dump/extraction-report.json
```

The [historical RSHD naming list](https://github.com/Rune-Status/lequietriot-RS-MIDI-Dumper/blob/master/RSHD%20MIDI%20Names.txt) is not authoritative for local archive numbers. For example, its label for archive 0 conflicts with the local name hash identifying Scape Main. Names were applied only through matching hashes; many remain numeric.

Do not force the 418 unmatched archives onto unrelated songs. Some may have no HDOS counterpart, while others may require verified aliases or better metadata. Investigate rather than assuming one cause.

## Native audio validation

All 1,222 HDOS MIDIs were tested serially for required instruments and a short PCM prefix, approximately 0.255 seconds each.

| Set | Passed loading and decode checks | Failed |
| --- | ---: | ---: |
| Index 6 music | 825 | 1 |
| Index 11 fanfares | 396 | 0 |

Music archive 280 requires instrument patch 658, absent from the index 15 reference table. The strict synth fails rather than silently skipping it. Compacting the cache does not recover missing data.

Silent introductions were investigated separately. Music 84 and 134 eventually produce sound. Music 171, music 765, and fanfare 245 produced zero PCM across their complete durations despite successful instrument loading. They may be placeholders; that interpretation is not proven. Do not manufacture replacement audio or label them corrupt without evidence.

A full Autumn Voyage render was also verified: 342.012517 seconds, stereo 44.1 kHz/16 bit, peak 12,672, RMS about 1,380. The original full-render SHA256 is `eacb3b476f7f062b9a3c01ab36ef2b4b13581ff385f4e19139d32955d63ad1c7`.

Reports in `hdos-render-work`, also copied into the original portable bundle:

```text
hdos-validation-summary.json
hdos-all-tracks-validation.csv
hdos-silent-prefix-followup.csv
hdos-silent-30sec-followup.csv
hdos-silent765-complete.csv
```

These results do not establish complete-duration listening verification for every song or exact perceptual equivalence to live HDOS.

## Live observations and restoration bug

The first development client loaded the plugin, detected live song names, retained OSRS music for the unmatched Call of the Tlati, and logged active native playback of the index 6 song named Fanfare. That song is not evidence of automatic index 11 jingle support.

The first launcher used RuneLite's stripped runtime API JAR and caused a missing `VarbitID` class in DevTools. The full portable bundle uses the full matching API JAR instead. Sideloaded event registration also emitted `LambdaConversionException` warnings while events were still delivered through RuneLite's apparent fallback path. Investigate supported compatibility rather than hiding warnings or modifying RuneLite internals.

The first playback implementation copied a slider-script technique from Music Replacer. RuneLite rejected `client.runScript(9238, ...)` with **Packet sent from script without permission**.

The current source removes these calls. `restoreOriginal()` uses only `Client.setMusicVolume`. A regression test failed before removal and passed afterward. **Do not restore the script technique or bypass packet restrictions.**

Immediate native-song resumption after that API-only restoration remains uncertain. It has not been fully reverified in the live game. If needed, the present README tells testers to toggle game music off/on manually. The next developer should resolve or accurately document this limitation through supported APIs.

Brandon stopped the earlier Computer Use session with physical Escape before planned testing was complete. No further UI automation was performed after that stop in that turn. Do not resume control of a live desktop or game session without considering current authorization. Do not reuse old process/window IDs.

## Portable launchers and packaging

The original full bundle contains:

```text
Start-HDOS-Music.cmd
Check-Installation.cmd
launcher.jar
plugin/hdos-music.jar
client/
java-runtime/
assets/cache/
assets/music/
assets/fanfares/
source/
reports/
checksums.sha256
START HERE.txt
THIRD-PARTY-NOTICES.txt
```

`packaging/Bootstrap.java` installs the plugin into the recipient's own `.runelite/sideloaded-plugins/hdos-music.jar`, backing up a different existing version. It launches the unmodified official RuneLite main class with developer mode and the `HDOS-Music-Portable` profile.

The bundle does not contain Brandon's login data. Recipients use their own supported login flow. Jagex Account compatibility on a separate computer remains unverified; do not extract or copy credentials to make a test pass.

The offline bootstrap check verifies assets and decodes one second of Autumn Voyage without logging in or installing the plugin. The full ZIP was extracted elsewhere, all 1,604 payload files were hash-verified, and the offline check passed using the bundled runtime and a clean simulated `user.home` that had no `.runelite` directory.

Java runtime modules were checked with `jdeps`. The correct staging runtime is `java-runtime`. An earlier incomplete `runtime` folder remains in staging and is deliberately excluded by `zip_and_verify.py`.

Development scripts `Install-Plugin.ps1` and `Launch-HDOS-RuneLite.ps1` are older local-development conveniences. The latter relies on installed RuneLite dependency files and Java on PATH. It is not the portable recipient launcher.

Packaging tools:

| File | Current purpose |
| --- | --- |
| `packaging/build_bundle.py` | Assemble the full bundle using explicit source whitelists |
| `packaging/zip_and_verify.py` | Create a ZIP, verify CRCs/hashes, extract, and run the offline check |
| `packaging/compact_soundbank.py` | Create and verify the smaller soundbank and MIDI assets ZIP |
| `packaging/Bootstrap.java` | Portable full-bundle launcher/checker |

These scripts have hard-coded versions/counts, local cache paths, and manual runtime-creation dependencies. They are prototype tooling, not a finished release pipeline. They intentionally refuse certain output overwrites. Version new artifacts or make replacement behavior explicit rather than deleting existing folders blindly.

## Recommended installation architecture

The latest concrete result supports a small plugin plus an automatically managed **compact native soundbank and MIDI package**, approximately 63 MB compressed in the current implementation.

Proposed sequence:

1. Player installs the plugin through the normal Plugin Hub workflow, once approved.
2. Plugin asynchronously obtains an approved, versioned data manifest from a fixed HTTPS origin.
3. It downloads and verifies only audio assets and mapping data, never executable code.
4. It installs the asset version atomically into plugin-owned storage and automatically selects its paths.
5. It keeps original OSRS music available while downloading or if verification/playback fails.
6. It reuses downloaded data across sessions and updates only when needed.

No hosting origin, provider, account, CDN, release endpoint, or asset permissions have been established. Do not invent an endpoint or provision/publish infrastructure without authorization.

Pre-rendered compressed audio remains an alternative if native synthesis cannot meet fidelity, licensing, or review requirements. It is not necessary merely to avoid shipping the full cache: that size problem has already been reduced by the compact soundbank. Measure encoded size and verify sound before making that architectural change.

The current plugin is not Plugin Hub-ready. Consult the current [Plugin Hub instructions](https://github.com/runelite/plugin-hub), [review policy](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review), and [example plugin guidelines](https://github.com/runelite/example-plugin/blob/master/AGENTS.md). Approval of an existing plugin does not guarantee that the same technique is accepted now.

## Licensing and distribution status

The native synth repository and its bundled Displee library had no included license establishing redistribution permission. Their permission status remains unresolved. Original code/assets and third-party notices are preserved, but that does not establish permission to publish them.

RuneScape music and cache assets are third-party content, including Jagex content. Hosting MIDI, compact samples, or rendered audio all require an appropriate distribution basis; changing the format does not settle that issue.

The existing ZIPs were assembled locally for requested testing. They are not represented as cleared for public hosting or Plugin Hub distribution. Resolve permissions, obtain an appropriate alternative, or consider a permitted own-cache import workflow before release. Do not publish or contact rights holders on Brandon's behalf without authorization.

## Continuation plan and acceptance tests

### 1. Establish the baseline

Read the active classes, build files, this README, and reports. Run the existing targeted tests and record the current Git state. Update stale project documentation and package metadata without confusing upstream and active sources.

Acceptance: another developer can build the actual plugin and identify what is implemented without consulting prior chat.

### 2. Adopt the compact soundbank

Parameterize the compactor. Reuse the existing cache reader and synth. Test all music/fanfares against the compact container, compare failures to the original, and verify original-cache immutability. Define a versioned asset manifest with sizes, hashes, compatibility, and provenance. Replace hard-coded file counts with manifest checks where appropriate.

Acceptance: a clean local installation plays from the compact assets with equivalent validated output and requires no full HDOS cache. Rebuild a distinctly versioned portable test package if requested; the current full ZIP still uses the old cache.

### 3. Fix playback lifecycle and restoration

Evidence priorities are mapped-to-mapped transitions, mapped-to-unmapped transitions, load failure, missing data, preview cancellation, rapid changes, disable/re-enable, volume/mute, logout/disconnect, and audio-device failure. Verify client/music volume scales instead of assuming them. Investigate restart behavior through supported APIs only.

Use injectable synthesis/output interfaces where needed to test late completion, cancellation, shutdown, and fallback without opening real audio hardware. Keep expensive work off the client thread and preserve single-synth serialization.

Acceptance: no stale replacement, double playback, stuck mute, client-thread blocking, or leaked audio device after transitions and shutdown. Check the original symptom in the running game.

### 4. Strengthen track identity and fidelity

Investigate supported music-change/identity data before depending permanently on widget text. Confirm behavior before/after first opening the music tab and while the tab is closed. Do not assume arbitrary varps identify archive IDs. Preserve mapping confidence and evidence; distinguish aliases, ambiguity, no counterpart, and missing data.

Compare representative songs with live HDOS: Scape Main, Autumn Voyage, Harmony, Sea Shanty, and songs with different MIDI arrangements. Check banks, percussion, tempo, envelopes, volume, looping, silent introductions, note-release tails, and the use of `isFinished()`.

Acceptance: every automatic replacement is traceable; sound equivalence and remaining differences are documented rather than inferred from nonzero PCM alone.

### 5. Implement automatic asset management

After distribution/hosting decisions, implement asynchronous verified downloads, bounded resource usage, atomic installation, retry/cancellation, clear progress, offline reuse, and original-music fallback. Handle corruption, interrupted downloads, insufficient space, moving folders, and asset-version mismatch. Do not load remotely downloaded classes/JARs.

Acceptance: a new player installs the plugin and requires no path configuration, separate Java, or HDOS installation. Normal login stays with their existing supported client setup.

### 6. Handle jingles as a separate feature

Find supported detection and identity hooks for actual index 11 jingles. Define interruption, music resumption, jingle volume, and prioritization. Do not equate a song named Fanfare with jingle support. If supported hooks cannot provide this, scope the feature honestly rather than bypassing client restrictions.

Acceptance: representative actual jingles replace and resume music correctly, or the release explicitly states that jingle replacement is unsupported.

### 7. Prepare release and second-machine testing

Resolve licensing/dependency review, make builds reproducible, use a compatible supported RuneLite runtime target, and eliminate the custom-launcher requirement for the intended Plugin Hub release. Verify installation, lifecycle behavior, and login on a genuinely separate Windows computer with its owner's account. Audit package contents for accidental user data.

Acceptance: tested installation and playback from a clean recipient environment, honest coverage/limitations, and a reproducible artifact. Publishing, uploads, Plugin Hub PRs, and external messages require separate authorization.

## Working constraints

Read Brandon's global and repository instructions. Make the smallest complete change that fixes an evidenced problem. Reuse existing mechanisms, preserve shared working-tree edits, and stage only owned changes when committing is authorized.

Never modify original cache data, run concurrent native synths within one JVM, bypass RuneLite packet checks, manufacture sessions, or extract credentials. Do not kill/restart a live game or control Brandon's desktop without clear current authorization. Passing unit tests, generating PCM, and starting a client are different from verifying actual in-game replacement behavior.

After each substantial phase, report what changed, what was observed, how it was checked, what failed, and what remains unverified. Do not call the current prototype or either ZIP a fully finished zero-configuration release.
