@echo off
rem Builds the chess game into out\game and copies PNG resources alongside the classes.
setlocal
if not exist "out\game" mkdir "out\game"
javac -encoding UTF-8 -d "out\game" src\*.java
if errorlevel 1 exit /b 1
copy /Y src\*.png "out\game" >nul
echo Build complete. Run with: java -cp out\game ChessGame
