@echo off
setlocal

set JAVA_BIN=
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA_BIN=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA_BIN if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\javaw.exe" set "JAVA_BIN=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\javaw.exe"
if not defined JAVA_BIN if exist "%~dp0dist\DeluxeMenusStudio\runtime\bin\javaw.exe" set "JAVA_BIN=%~dp0dist\DeluxeMenusStudio\runtime\bin\javaw.exe"
if not defined JAVA_BIN set "JAVA_BIN=javaw"

set "JAR_FILE=%~dp0target\deluxemenus-studio-1.3.1.jar"
if not exist "%JAR_FILE%" goto :need_build

:launch
echo [DeluxeMenus Studio] Starting via: %JAVA_BIN%
start "" "%JAVA_BIN%" -jar "%JAR_FILE%"
exit /b 0

:need_build
echo [DeluxeMenus Studio] JAR not found. Building...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [DeluxeMenus Studio] Build failed. Ensure JDK 21+ and Maven are installed.
    pause
    exit /b 1
)
goto :launch
