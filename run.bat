@echo off
chcp 65001 > nul
title DeluxeMenus Studio
echo Запуск DeluxeMenus Studio...

if not exist "%~dp0target\deluxemenus-studio-1.0.0-SNAPSHOT.jar" (
    echo Файл JAR не найден, компилируем проект...
    call mvn clean package -DskipTests
)

start javaw -jar "%~dp0target\deluxemenus-studio-1.0.0-SNAPSHOT.jar"
