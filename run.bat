@echo off
setlocal enabledelayedexpansion
chcp 65001 > nul
title DeluxeMenus Studio

set "JAR_FILE=%~dp0target\deluxemenus-studio-1.1.0.jar"
if not exist "%JAR_FILE%" (
    for %%f in ("%~dp0target\deluxemenus-studio-*.jar") do (
        set "JAR_FILE=%%f"
    )
)

if not exist "!JAR_FILE!" (
    echo [DeluxeMenus Studio] JAR не найден. Выполняется сборка...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo [DeluxeMenus Studio] Ошибка сборки. Убедитесь, что установлены JDK 21+ и Maven.
        pause
        exit /b 1
    )
)

echo [DeluxeMenus Studio] Запуск приложения...
start javaw -jar "!JAR_FILE!"
