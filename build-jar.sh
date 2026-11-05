#!/bin/sh
# Builds RestaurantRush.jar: double-click it to open the game window,
# or run the console version with: java -cp RestaurantRush.jar rush.Main
cd "$(dirname "$0")"
rm -rf out-jar && mkdir -p out-jar
javac --release 11 -d out-jar $(find src -name "*.java") || exit 1
printf 'Main-Class: rush.gui.GuiMain\n' > out-jar/manifest.txt
jar cfm RestaurantRush.jar out-jar/manifest.txt -C out-jar rush
rm -rf out-jar
echo "Built RestaurantRush.jar"
