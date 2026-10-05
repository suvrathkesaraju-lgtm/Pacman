#!/usr/bin/env bash
# Builds build/Pacman.jar, a runnable JAR that works on any OS with Java 17+.
set -euo pipefail
cd "$(dirname "$0")"

rm -rf build
mkdir -p build/classes

javac -d build/classes src/*.java
cp src/*.png build/classes/
jar --create --file build/Pacman.jar --main-class App -C build/classes .

echo "Built build/Pacman.jar"
echo "Run it with: java -jar build/Pacman.jar"
