@echo off
title SmartCampus AI — Launcher
echo ========================================================
echo   Starting SmartCampus AI Platform (Java 21 LTS)
echo   Architecture: Ponytail Stdlib (Zero-Dependency)
echo ========================================================
echo.

:: Check for Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not detected in your system PATH.
    echo Please install Java 17 or Java 21 from: https://adoptium.net/
    pause
    exit /b 1
)

:: Compile if needed
echo [1/2] Compiling Java backend...
if not exist "bin" mkdir bin
javac -d bin src/SmartCampusApp.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b 1
)

:: Open browser automatically after 1.5 seconds
start "" cmd /c "timeout /t 2 /nobreak >nul && start http://localhost:8080"

:: Start the server
echo [2/2] Launching server on http://localhost:8080...
echo.
echo Press Ctrl+C in this terminal to stop the server anytime.
echo.
java -cp bin src.SmartCampusApp
pause
