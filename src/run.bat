@echo off
chcp 65001 >nul
del Main.class 2>nul
javac -encoding UTF-8 Main.java
if errorlevel 1 (
    echo === BIEN DICH LOI ===
    pause
    exit /b
)
java -Dfile.encoding=UTF-8 Main
pause