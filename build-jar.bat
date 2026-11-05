@echo off
REM Builds RestaurantRush.jar (double-click to play; console: java -cp RestaurantRush.jar rush.Main)
cd /d "%~dp0"
if exist out-jar rmdir /s /q out-jar
mkdir out-jar
javac --release 11 -d out-jar -sourcepath src src\rush\Main.java src\rush\gui\GuiMain.java
if errorlevel 1 exit /b 1
echo Main-Class: rush.gui.GuiMain> out-jar\manifest.txt
jar cfm RestaurantRush.jar out-jar\manifest.txt -C out-jar rush
rmdir /s /q out-jar
echo Built RestaurantRush.jar
