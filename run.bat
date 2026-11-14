@echo off
REM Compile and run Restaurant Rush on Windows.
REM Usage: run.bat            (interactive)
REM        run.bat --echo < tests\stage3-victory.txt   (scripted run)
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
REM -sourcepath lets javac find every class from Main, so no file list is needed
REM (this also works when the folder path contains spaces).
javac -d out -sourcepath src src\rush\Main.java
if errorlevel 1 exit /b 1
java -cp out rush.Main %*
