#Requires -Version 5.1
# Removes the 2008 Music Soundtrack tester plugin and its desktop shortcut.
# Does NOT delete downloaded music assets (~/.runelite/music2008/).
# Does NOT touch your RuneLite account, profile, or other plugins.

$JarPath      = Join-Path $env:USERPROFILE '.runelite\sideloaded-plugins\2008-music-soundtrack.jar'
$ShortcutPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'RuneLite - 2008 Music Test.lnk'
$AssetsDir    = Join-Path $env:USERPROFILE '.runelite\music2008'

Write-Host ''
Write-Host '=== 2008 Music Soundtrack - Tester Uninstaller ===' -ForegroundColor Cyan
Write-Host ''

if (Test-Path $JarPath)      { Remove-Item $JarPath;      Write-Host "[ok] Removed plugin JAR" }
else                         {                              Write-Host "[ ] Plugin JAR not found (already removed?)" }

if (Test-Path $ShortcutPath) { Remove-Item $ShortcutPath; Write-Host "[ok] Removed desktop shortcut" }
else                         {                              Write-Host "[ ] Shortcut not found" }

Write-Host ''
Write-Host "Music assets are kept at: $AssetsDir"
Write-Host "Delete that folder manually if you want to free the ~55 MB."
Write-Host ''
Write-Host 'Uninstall complete. Launch RuneLite normally to play without the plugin.' -ForegroundColor Green
Write-Host ''
