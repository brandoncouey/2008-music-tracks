@echo off
setlocal
cd /d "%~dp0"
"java-runtime\bin\java.exe" -ea -Xmx1024m "-Dhdos.bundle=%~dp0." -cp "launcher.jar;client\*" com.brandon.hdos.launcher.Bootstrap --check
pause
