#!/bin/bash

# Detect current architecture
CURRENT_ARCH=$(uname -m)

# Parse arguments
TARGET_ARCH=""
if [ "$1" == "intel" ] || [ "$1" == "x64" ] || [ "$1" == "x86_64" ]; then
    TARGET_ARCH="intel"
elif [ "$1" == "arm" ] || [ "$1" == "aarch64" ] || [ "$1" == "arm64" ]; then
    TARGET_ARCH="arm"
elif [ "$1" == "both" ]; then
    TARGET_ARCH="both"
fi

# Default to current architecture
if [ -z "$TARGET_ARCH" ]; then
    if [ "$CURRENT_ARCH" == "arm64" ]; then
        TARGET_ARCH="arm"
    else
        TARGET_ARCH="intel"
    fi
fi

build_for_arch() {
    local ARCH=$1
    local PROFILE=""
    local SUFFIX=""

    if [ "$ARCH" == "intel" ]; then
        PLATFORM_PROP="-Djavafx.platform=mac"
        SUFFIX="-intel"
        echo "Building for Intel Mac (x86_64)..."
    else
        PLATFORM_PROP="-Djavafx.platform=mac-aarch64"
        SUFFIX="-arm"
        echo "Building for Apple Silicon (ARM64)..."
    fi

    echo ""
    echo "Step 1: Cleaning and packaging..."
    mvn clean package -DskipTests $PLATFORM_PROP

    if [ $? -ne 0 ]; then
        echo "Build failed!"
        return 1
    fi

    echo ""
    echo "Step 2: Preparing for native packaging..."
    mkdir -p target/jpackage-input
    cp target/vibe-tanks-1.0-SNAPSHOT-shaded.jar target/jpackage-input/

    # Determine output name
    if [ "$TARGET_ARCH" == "both" ]; then
        APP_NAME="VibeTanks${SUFFIX}"
    else
        APP_NAME="VibeTanks"
    fi

    # Check if cross-compiling (building for different architecture than current machine)
    local CROSS_COMPILE=false
    if [ "$ARCH" == "intel" ] && [ "$CURRENT_ARCH" == "arm64" ]; then
        CROSS_COMPILE=true
        echo ""
        echo "Note: Creating Intel JAR on ARM Mac."
        echo "The .app bundle will only work on Intel Macs if you have an Intel JDK."
        echo "The JAR can run on any Intel Mac with Java installed."
    elif [ "$ARCH" == "arm" ] && [ "$CURRENT_ARCH" == "x86_64" ]; then
        CROSS_COMPILE=true
        echo ""
        echo "Note: Creating ARM JAR on Intel Mac."
        echo "The .app bundle will only work on ARM Macs if you have an ARM JDK."
        echo "The JAR can run on any ARM Mac with Java installed."
    fi

    echo ""
    echo "Step 3: Creating native app image ($APP_NAME)..."
    jpackage --name "$APP_NAME" --app-version 1.0.0 --vendor VibeTanks --dest target/dist --input target/jpackage-input --main-jar vibe-tanks-1.0-SNAPSHOT-shaded.jar --main-class com.vibetanks.Launcher --type app-image --mac-package-identifier com.vibetanks.app

    if [ $? -ne 0 ]; then
        echo "Native packaging failed!"
        echo ""
        echo "Note: jpackage requires JDK 14 or later with jpackage tool."
        echo ""
        echo "You can run the shaded JAR directly:"
        echo "  java -jar target/vibe-tanks-1.0-SNAPSHOT-shaded.jar"
        return 1
    fi

    if [ "$CROSS_COMPILE" == true ]; then
        echo ""
        echo "WARNING: Cross-compiled .app may not work correctly."
        echo "The JAR file is platform-independent and can be distributed."
    fi

    echo ""
    echo "Step 4: Code signing the app..."
    codesign --force --deep --sign - "target/dist/${APP_NAME}.app"
    xattr -cr "target/dist/${APP_NAME}.app"

    if [ "$TARGET_ARCH" != "both" ]; then
        echo ""
        echo "Step 5: Creating alias in project folder..."
        SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
        ln -sf "$SCRIPT_DIR/target/dist/${APP_NAME}.app" "$SCRIPT_DIR/VibeTanks.app"
    fi

    return 0
}

echo "========================================"
echo "Building VibeTanks for macOS"
echo "Current machine: $CURRENT_ARCH"
echo "Target: $TARGET_ARCH"
echo "========================================"

if [ "$TARGET_ARCH" == "both" ]; then
    echo ""
    echo "========== Building Intel version =========="
    build_for_arch "intel"
    INTEL_RESULT=$?

    # Preserve Intel app before ARM build cleans target
    if [ $INTEL_RESULT -eq 0 ]; then
        mkdir -p /tmp/vibetanks-build
        cp -R target/dist/VibeTanks-intel.app /tmp/vibetanks-build/
    fi

    echo ""
    echo "========== Building ARM version =========="
    build_for_arch "arm"
    ARM_RESULT=$?

    # Restore Intel app after ARM build
    if [ $INTEL_RESULT -eq 0 ]; then
        cp -R /tmp/vibetanks-build/VibeTanks-intel.app target/dist/
        rm -rf /tmp/vibetanks-build
    fi

    echo ""
    echo "========================================"
    echo "Build complete!"
    echo ""
    if [ $INTEL_RESULT -eq 0 ]; then
        echo "Intel version: target/dist/VibeTanks-intel.app"
    else
        echo "Intel version: FAILED"
    fi
    if [ $ARM_RESULT -eq 0 ]; then
        echo "ARM version: target/dist/VibeTanks-arm.app"
    else
        echo "ARM version: FAILED"
    fi
    echo ""
    echo "Note: .app bundles only run on their target architecture."
    echo "The JAR can run on any Mac with Java installed."
    echo "========================================"
else
    build_for_arch "$TARGET_ARCH"

    echo ""
    echo "========================================"
    echo "Build complete!"
    echo ""
    echo "Alias created: ./VibeTanks.app (in project root)"
    echo "App bundle located in: target/dist/VibeTanks.app"
    echo ""
    echo "You can also run the JAR directly with:"
    echo "  java -jar target/vibe-tanks-1.0-SNAPSHOT-shaded.jar"
    echo "========================================"
fi

echo ""
echo "Usage: ./build-mac.sh [intel|arm|both]"
echo "  intel - Build for Intel Mac (x86_64)"
echo "  arm   - Build for Apple Silicon (ARM64)"
echo "  both  - Build both versions"
echo "  (default: current architecture)"
