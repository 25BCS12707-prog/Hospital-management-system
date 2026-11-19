#!/bin/sh
# Compile and run (Linux / macOS)
mkdir -p out
javac -d out -sourcepath src src/hospital/Main.java && java -cp out hospital.Main
