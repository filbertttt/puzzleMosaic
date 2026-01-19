#!/bin/bash

echo "Compiling Java files..."

# Create the bin directory if it doesn't exist
mkdir -p bin

# Compile Java files from src to bin
# Note: macOS uses / for paths
javac -d bin src/*.java

# Check the exit status ($? is the equivalent of %errorlevel%)
if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo "Run with: java -cp bin MultiSizeExperimentRunner"
else
    echo "Compilation failed!"
fi

# 'pause' isn't a command in macOS; this achieves the same effect:
read -p "Press [Enter] to continue..."
