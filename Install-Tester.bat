@echo off
setlocal enabledelayedexpansion
title 2008 Music Soundtrack - Installer
set "LOG=%TEMP%\2008-music-install.log"
call :main > "%LOG%" 2>&1
type "%LOG%"
pause
exit /b

:main
echo.
echo === 2008 Music Soundtrack - Tester Installer ===
echo.

:: ── 1. Verify RuneLite is installed ──────────────────────────────────────────
set "RUNELITEJAR=%LOCALAPPDATA%\RuneLite\RuneLite.jar"
set "JAVAW=%LOCALAPPDATA%\RuneLite\jre\bin\javaw.exe"

if not exist "%RUNELITEJAR%" (
    echo [ERROR] RuneLite.jar not found at: %RUNELITEJAR%
    echo         Please install RuneLite from https://runelite.net first.
    goto :error
)
if not exist "%JAVAW%" (
    echo [ERROR] RuneLite JRE not found at: %JAVAW%
    echo         Please reinstall RuneLite from https://runelite.net
    goto :error
)
echo [ok] Found RuneLite.jar: %RUNELITEJAR%

:: ── 2. Create sideloaded-plugins folder ──────────────────────────────────────
set "PLUGINDIR=%USERPROFILE%\.runelite\sideloaded-plugins"
if not exist "%PLUGINDIR%" (
    mkdir "%PLUGINDIR%"
    echo [ok] Created: %PLUGINDIR%
) else (
    echo [ok] Plugin folder exists: %PLUGINDIR%
)

:: ── 3. Remove any old JARs that could conflict ───────────────────────────────
set "JARDEST=%PLUGINDIR%\2008-music-soundtrack.jar"
if exist "%JARDEST%" (
    del "%JARDEST%" >nul
    echo [ok] Removed old plugin JAR
)
:: Remove any stale plugin JARs that might conflict
for %%F in ("%PLUGINDIR%\*.jar") do (
    if /i not "%%~nxF"=="2008-music-soundtrack.jar" del "%%F" >nul 2>&1
)

:: ── 4. Download plugin JAR ────────────────────────────────────────────────────
echo.
echo Downloading plugin JAR...
set "JARURL=https://github.com/brandoncouey/2008-music-tracks/releases/latest/download/2008-music-soundtrack.jar"
powershell -NoProfile -Command "(New-Object Net.WebClient).DownloadFile('%JARURL%', '%JARDEST%')"
if not exist "%JARDEST%" (
    echo [ERROR] Download failed. Check your internet connection or download manually:
    echo         %JARURL%
    goto :error
)
for %%F in ("%JARDEST%") do set "SIZE=%%~zF"
set /a SIZE=%SIZE%/1024
echo [ok] Downloaded plugin JAR (%SIZE% KB)

:: ── 5. Write a launcher bat and point shortcut at it ─────────────────────────
echo.
echo Creating launcher...
set "LAUNCHERBAT=%LOCALAPPDATA%\RuneLite\launch-dev.bat"
set "DESKTOP=%USERPROFILE%\OneDrive\Desktop"
if not exist "%DESKTOP%" set "DESKTOP=%USERPROFILE%\Desktop"
set "SHORTCUT=%DESKTOP%\RuneLite - 2008 Music Test.lnk"

(
echo @echo off
echo "%JAVAW%" -jar "%RUNELITEJAR%" --developer-mode
) > "%LAUNCHERBAT%"
echo [ok] Created launcher: %LAUNCHERBAT%

set "PATCHPS1=%TEMP%\make-runelite-shortcut.ps1"
(
echo $sh = New-Object -COM WScript.Shell
echo $lnk = $sh.CreateShortcut('%SHORTCUT%'^)
echo $lnk.TargetPath = '%LAUNCHERBAT%'
echo $lnk.WorkingDirectory = '%LOCALAPPDATA%\RuneLite'
echo $lnk.Description = 'RuneLite with 2008 Music Soundtrack'
echo $lnk.WindowStyle = 7
echo $lnk.Save(^)
echo Write-Host '[ok] Created desktop shortcut'
) > "%PATCHPS1%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%PATCHPS1%"
del "%PATCHPS1%" >nul 2>&1

echo.
echo === Installation complete! ===
echo.
echo Next steps:
echo   1. Use "RuneLite - 2008 Music Test" on your desktop to launch RuneLite.
echo      (Use this shortcut instead of Jagex Launcher while testing.)
echo   2. Log in normally with your own account.
echo   3. Open Plugin Hub and enable "2008 Music Soundtrack" if not already on.
echo   4. The plugin downloads ~63 MB of music assets the first time.
echo   5. Walk to a new area or open the Music tab - 2008 music plays!
echo.
echo To uninstall: run Uninstall-Tester.bat
echo Report issues: https://github.com/brandoncouey/2008-music-tracks/issues
echo.
exit /b 0

:error
echo.
exit /b 1
