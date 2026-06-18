#!/usr/bin/env bash
# Compile + run the generator. Requires a JDK (javac) on PATH.
set -e
mkdir -p out
javac -d out src/main/java/com/poc/testdata/TestDataGenerator.java
java -cp out com.poc.testdata.TestDataGenerator "$@"
