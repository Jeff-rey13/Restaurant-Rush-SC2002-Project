#!/bin/sh
# Compile and open the game window. Options: --replay tests/<script>.txt --speed 2 --mute --start
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -d out $(find src -name "*.java") || exit 1
java -cp out rush.gui.GuiMain "$@"
