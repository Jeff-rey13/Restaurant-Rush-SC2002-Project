#!/bin/sh
# Compile and run Restaurant Rush. Usage: ./run.sh            (interactive)
#                                         ./run.sh --echo < tests/xxx.txt   (scripted run)
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
javac -d out $(find src -name "*.java") || exit 1
java -cp out rush.Main "$@"
