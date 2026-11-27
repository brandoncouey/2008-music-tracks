#Requires -Version 5.1
<#
.SYNOPSIS
    Installs the 2008 Music Soundtrack RuneLite plugin for testing.
.DESCRIPTION
    Downloads the plugin JAR from GitHub, places it in your RuneLite
    sideloaded-plugins folder, and creates a desktop shortcut that
    launches RuneLite with developer mode enabled.

    Music assets (~63 MB) download automatically the first time you
    enable the plugin inside RuneLite.

    Safe to run multiple times - existing files are backed up.
.NOTES
    Requires RuneLite to already be installed (https://runelite.net).
    Does NOT touch your RuneLite account, login, or any game settings.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$JarUrl      = 'https://github.com/brandoncouey/hdos-music-plugin/releases/latest/download/2008-music-soundtrack.jar'
$PluginDir   = Join-Path $env:USERPROFILE '.runelite\sideloaded-plugins'
$JarDest     = Join-Path $PluginDir '2008-music-soundtrack.jar'
$RuneLiteExe = $null

Write-Host ''
Write-Host '=== 2008 Music Soundtrack - Tester Installer ===' -ForegroundColor Cyan
Write-Host ''

# ── 1. Find RuneLite ──────────────────────────────────────────────────────────
$candidates = @(
    (Join-Path $env:LOCALAPPDATA 'RuneLite\RuneLite.exe'),
    (Join-Path $env:PROGRAMFILES 'RuneLite\RuneLite.exe'),
    (Join-Path ${env:PROGRAMFILES(x86)} 'RuneLite\RuneLite.exe')
)
foreach ($c in $candidates) {
    if (Test-Path $c) { $RuneLiteExe = $c; break }
}
if (-not $RuneLiteExe) {
    # Last resort: search common places
    $found = Get-ChildItem "$env:LOCALAPPDATA" -Filter 'RuneLite.exe' -Recurse -ErrorAction SilentlyContinue |
             Select-Object -First 1
    if ($found) { $RuneLiteExe = $found.FullName }
}
if (-not $RuneLiteExe) {
    Write-Host '[!] Could not find RuneLite.exe automatically.' -ForegroundColor Yellow
    Write-Host '    Please enter the full path to RuneLite.exe:' -ForegroundColor Yellow
    $RuneLiteExe = Read-Host '    Path'
    if (-not (Test-Path $RuneLiteExe)) {
        Write-Error "File not found: $RuneLiteExe"
    }
}
Write-Host "[ok] Found RuneLite: $RuneLiteExe"

# ── 2. Create sideloaded-plugins folder ───────────────────────────────────────
if (-not (Test-Path $PluginDir)) {
    New-Item -ItemType Directory -Path $PluginDir | Out-Null
    Write-Host "[ok] Created: $PluginDir"
} else {
    Write-Host "[ok] Plugin folder exists: $PluginDir"
}

# ── 3. Back up existing JAR if different ─────────────────────────────────────
if (Test-Path $JarDest) {
    $backup = $JarDest -replace '\.jar$', ('.backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.jar')
    Copy-Item $JarDest $backup
    Write-Host "[ok] Backed up existing JAR to: $(Split-Path $backup -Leaf)"
}

# ── 4. Download plugin JAR ────────────────────────────────────────────────────
Write-Host ''
Write-Host 'Downloading plugin JAR...' -ForegroundColor Cyan
try {
    $wc = New-Object System.Net.WebClient
    $wc.DownloadFile($JarUrl, $JarDest)
    $size = [math]::Round((Get-Item $JarDest).Length / 1KB)
    Write-Host "[ok] Downloaded plugin JAR (${size} KB) to: $JarDest"
} catch {
    Write-Error "Download failed: $_`n`nCheck your internet connection or download manually from:`n$JarUrl"
}

# ── 5. Create desktop shortcut ───────────────────────────────────────────────
$ShortcutPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'RuneLite - 2008 Music Test.lnk'
$WshShell     = New-Object -ComObject WScript.Shell
$shortcut     = $WshShell.CreateShortcut($ShortcutPath)
$shortcut.TargetPath       = $RuneLiteExe
$shortcut.Arguments        = '--developer-mode'
$shortcut.WorkingDirectory = Split-Path $RuneLiteExe
$shortcut.Description      = 'RuneLite with 2008 Music Soundtrack tester plugin'
$shortcut.Save()
Write-Host "[ok] Created desktop shortcut: RuneLite - 2008 Music Test.lnk"

# ── 6. Done ───────────────────────────────────────────────────────────────────
Write-Host ''
Write-Host '=== Installation complete! ===' -ForegroundColor Green
Write-Host ''
Write-Host 'Next steps:' -ForegroundColor White
Write-Host '  1. Double-click  "RuneLite - 2008 Music Test"  on your desktop.'
Write-Host '  2. Log in normally with your own account.'
Write-Host '  3. Open Plugin Hub (the puzzle-piece icon) and enable'
Write-Host '     "2008 Music Soundtrack" if it is not already on.'
Write-Host '  4. The plugin downloads ~63 MB of music assets the first time.'
Write-Host '     Watch the sidebar panel for progress.'
Write-Host '  5. Walk to a new area or open the Music tab - HDOS music plays!'
Write-Host ''
Write-Host 'Report issues at: https://github.com/brandoncouey/hdos-music-plugin/issues'
Write-Host ''
