#!/bin/sh
# Compile the game plus the rule tests and run them.
cd "$(dirname "$0")"
rm -rf out-test
mkdir -p out-test
javac -d out-test $(find src test -name "*.java") || exit 1
java -cp out-test rush.RuleTests
