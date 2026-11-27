@echo off
setlocal enabledelayedexpansion
title 2008 Music Soundtrack - Uninstaller
set "LOG=%TEMP%\2008-music-uninstall.log"
call :main > "%LOG%" 2>&1
type "%LOG%"
pause
exit /b

:main
echo.
echo === 2008 Music Soundtrack - Tester Uninstaller ===
echo.

:: ── 1. Remove plugin JAR ──────────────────────────────────────────────────────
set "JARDEST=%USERPROFILE%\.runelite\sideloaded-plugins\2008-music-soundtrack.jar"
if exist "%JARDEST%" (
    del "%JARDEST%"
    echo [ok] Removed plugin JAR
) else (
    echo [ ] Plugin JAR not found (already removed?)
)

:: ── 2. Remove launch-dev.bat ──────────────────────────────────────────────────
set "LAUNCHERBAT=%LOCALAPPDATA%\RuneLite\launch-dev.bat"
if exist "%LAUNCHERBAT%" (
    del "%LAUNCHERBAT%"
    echo [ok] Removed launch-dev.bat
) else (
    echo [ ] launch-dev.bat not found (already removed?)
)

:: ── 3. Remove desktop shortcut ────────────────────────────────────────────────
set "SHORTCUT1=%USERPROFILE%\OneDrive\Desktop\RuneLite - 2008 Music Test.lnk"
set "SHORTCUT2=%USERPROFILE%\Desktop\RuneLite - 2008 Music Test.lnk"
if exist "%SHORTCUT1%" (
    del "%SHORTCUT1%"
    echo [ok] Removed desktop shortcut
) else if exist "%SHORTCUT2%" (
    del "%SHORTCUT2%"
    echo [ok] Removed desktop shortcut
) else (
    echo [ ] Desktop shortcut not found (already removed?)
)

echo.
echo Music assets are kept at: %USERPROFILE%\.runelite\music2008
echo Delete that folder manually if you want to free the ~63 MB of space.
echo.
echo Uninstall complete. Launch RuneLite normally to play without the plugin.
echo.
exit /b 0
