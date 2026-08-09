#!/usr/bin/env bash
# The Margins 2D — double-click to play.
#
# Runs the game as a standalone jar (no Maven needed at play time). It starts
# from its own folder so the game finds its assets/ directory, no matter where
# you launch this file from. If the jar isn't built yet, it builds it once
# (takes about a minute the first time).

set -e
cd "$(dirname "$0")"

JAR="desktop/target/desktop-1.0-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
  echo "Building the game for the first time — this takes about a minute…"
  mvn -o -q -pl core install
  mvn -o -q -pl desktop package
fi

exec java -jar "$JAR"
