@echo off
REM Compile and open the game window. Options: --replay tests\stage1-victory.txt --speed 2 --mute --start
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac -d out -sourcepath src src\rush\Main.java src\rush\gui\GuiMain.java
if errorlevel 1 exit /b 1
start "" javaw -cp out rush.gui.GuiMain %*
