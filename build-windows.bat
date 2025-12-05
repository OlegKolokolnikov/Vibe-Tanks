@echo off
echo ========================================
echo Building VibeTanks for Windows
echo ========================================

echo.
echo Step 1: Cleaning and packaging...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Preparing for native packaging...
if not exist target\jpackage-input mkdir target\jpackage-input
copy /Y target\vibe-tanks-1.0-SNAPSHOT-shaded.jar target\jpackage-input\

echo.
echo Step 3: Creating native app image...
jpackage --name VibeTanks --app-version 1.0.0 --vendor VibeTanks --dest target\dist --input target\jpackage-input --main-jar vibe-tanks-1.0-SNAPSHOT-shaded.jar --main-class com.vibetanks.Launcher --type app-image

if %ERRORLEVEL% neq 0 (
    echo Native packaging failed!
    echo.
    echo Note: jpackage requires JDK 14 or later with jpackage tool.
    echo.
    echo Alternatively, you can run the shaded JAR directly:
    echo   java -jar target\vibe-tanks-1.0-SNAPSHOT-shaded.jar
    pause
    exit /b 1
)

echo.
echo Step 4: Creating shortcut in project folder...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%~dp0VibeTanks.lnk'); $Shortcut.TargetPath = '%~dp0target\dist\VibeTanks\VibeTanks.exe'; $Shortcut.WorkingDirectory = '%~dp0target\dist\VibeTanks'; $Shortcut.Save()"

echo.
echo ========================================
echo Build complete!
echo.
echo Shortcut created: VibeTanks.lnk (in project root)
echo Executable located in: target\dist\VibeTanks\
echo.
echo You can also run the JAR directly with:
echo   java -jar target\vibe-tanks-1.0-SNAPSHOT-shaded.jar
echo ========================================
pause
