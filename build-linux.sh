#!/bin/bash
echo "========================================"
echo "Building VibeTanks for Linux"
echo "========================================"

echo ""
echo "Step 1: Cleaning and packaging..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo ""
echo "Step 2: Preparing for native packaging..."
mkdir -p target/jpackage-input
cp target/vibe-tanks-1.0-SNAPSHOT-shaded.jar target/jpackage-input/

echo ""
echo "Step 3: Creating native app image..."
jpackage --name VibeTanks --app-version 1.0.0 --vendor VibeTanks --dest target/dist --input target/jpackage-input --main-jar vibe-tanks-1.0-SNAPSHOT-shaded.jar --main-class com.vibetanks.Launcher --type app-image

if [ $? -ne 0 ]; then
    echo "Native packaging failed!"
    echo ""
    echo "Note: jpackage requires JDK 14 or later with jpackage tool."
    echo ""
    echo "Alternatively, you can run the shaded JAR directly:"
    echo "  java -jar target/vibe-tanks-1.0-SNAPSHOT-shaded.jar"
    exit 1
fi

echo ""
echo "Step 4: Creating symlink in project folder..."
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ln -sf "$SCRIPT_DIR/target/dist/VibeTanks/bin/VibeTanks" "$SCRIPT_DIR/VibeTanks"
chmod +x "$SCRIPT_DIR/VibeTanks"

echo ""
echo "========================================"
echo "Build complete!"
echo ""
echo "Symlink created: ./VibeTanks (in project root)"
echo "Executable located in: target/dist/VibeTanks/bin/"
echo ""
echo "You can also run the JAR directly with:"
echo "  java -jar target/vibe-tanks-1.0-SNAPSHOT-shaded.jar"
echo "========================================"
