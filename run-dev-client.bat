@echo off
setlocal

REM ---------------------------------------------------------------------------
REM  Oathfall - launch a development RuneLite client with the plugin loaded.
REM
REM  Double-click this file, or run it from a terminal. First run downloads
REM  Gradle and the RuneLite client and can take a few minutes; later runs are
REM  fast. The window stays open on failure so you can read the error.
REM ---------------------------------------------------------------------------

cd /d "%~dp0"

echo.
echo   OATHFALL - development client
echo   ------------------------------------------------------------
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo   [x] Java is not on your PATH.
    echo       RuneLite needs JDK 11. Get it from https://adoptium.net/temurin/releases/
    echo.
    pause
    exit /b 1
)

for /f "tokens=*" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do echo   Java:  %%v
echo   Plugin: %cd%
echo.
echo   Building and launching. The game window opens when it is ready.
echo   Close the game window, or press Ctrl+C here, to stop.
echo.

call gradlew.bat run --console=plain
set EXITCODE=%ERRORLEVEL%

echo.
if not "%EXITCODE%"=="0" (
    echo   [x] The client exited with code %EXITCODE%.
    echo       Scroll up for the cause. Common ones:
    echo         - no internet on first run ^(Gradle/RuneLite must download^)
    echo         - a Jagex account needs the extra login step, see
    echo           https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts
    echo.
    pause
    exit /b %EXITCODE%
)

echo   Client closed cleanly.
echo.
pause
endlocal
