#!/usr/bin/env bash
# Vercel build for the Web (wasmJs) target. The Vercel build image has Node but no JDK,
# so a JDK 17 is downloaded first. Variables come from the project's environment (.env).
# The Android SDK is not needed: the wasmJs tasks configure and build without it.
set -euo pipefail

: "${JDK_URL:=https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse}"
: "${GRADLE_TASK:=:composeApp:wasmJsBrowserDistribution}"
: "${GRADLE_JVMARGS:=-Xmx3g -XX:MaxMetaspaceSize=1g -Dfile.encoding=UTF-8}"

JDK_DIR="${JDK_DIR:-$HOME/.cache/fastpilot-jdk}"
if [ ! -x "$JDK_DIR/bin/java" ]; then
  echo "Downloading JDK from $JDK_URL"
  mkdir -p "$JDK_DIR"
  curl -fsSL "$JDK_URL" | tar -xz --strip-components=1 -C "$JDK_DIR"
fi
export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

chmod +x ./gradlew
./gradlew "$GRADLE_TASK" --no-daemon --stacktrace -Dorg.gradle.jvmargs="$GRADLE_JVMARGS"
