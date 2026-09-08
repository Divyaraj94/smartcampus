#!/bin/bash
# SmartCampus AI — Launcher for macOS and Linux

echo "========================================================"
echo "  Starting SmartCampus AI Platform (Java 21 LTS)"
echo "  Architecture: Ponytail Stdlib (Zero-Dependency)"
echo "========================================================"

# Check for Java
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH."
    echo "Please install Java 17 or 21: https://adoptium.net/"
    exit 1
fi

echo "[1/2] Compiling Java backend..."
mkdir -p bin
javac -d bin src/SmartCampusApp.java

if [ $? -ne 0 ]; then
    echo "[ERROR] Compilation failed."
    exit 1
fi

echo "[2/2] Launching server on http://localhost:8080..."
echo "Press Ctrl+C to stop the server anytime."
echo ""

# Attempt to open browser automatically
if command -v open &> /dev/null; then
    (sleep 1.5 && open http://localhost:8080) &
elif command -v xdg-open &> /dev/null; then
    (sleep 1.5 && xdg-open http://localhost:8080) &
fi

java -cp bin src.SmartCampusApp
