@echo off
setlocal
cd /d "%~dp0"
if not exist "java-runtime\bin\java.exe" (
  echo Extract the entire ZIP first. The bundled Java runtime is missing.
  pause
  exit /b 1
)
"java-runtime\bin\java.exe" -ea -Xmx1024m "-Dhdos.bundle=%~dp0." -Dsun.java2d.d3d=false -cp "launcher.jar;client\*" com.brandon.hdos.launcher.Bootstrap
if errorlevel 1 pause
