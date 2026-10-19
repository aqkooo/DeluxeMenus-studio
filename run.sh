#!/usr/bin/env bash
# DeluxeMenus Studio - Linux Launcher
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if ! command -v java >/dev/null 2>&1; then
    echo "[DeluxeMenus Studio] Ошибка: Java не найдена в PATH." >&2
    echo "[DeluxeMenus Studio] Требуется Java Runtime Environment (JRE/JDK) версии 21 или выше." >&2
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ -n "$JAVA_VER" ] && [ "$JAVA_VER" -lt 21 ]; then
    echo "[DeluxeMenus Studio] Предупреждение: обнаружена Java $JAVA_VER. Рекомендуется Java 21+." >&2
fi

JAR_FILE=""
if [ -f "$SCRIPT_DIR/target/deluxemenus-studio-1.3.1.jar" ]; then
    JAR_FILE="$SCRIPT_DIR/target/deluxemenus-studio-1.3.1.jar"
else
    CANDIDATE=$(ls -1 "$SCRIPT_DIR/target"/deluxemenus-studio-*.jar 2>/dev/null | grep -v 'original-' | head -n 1 || true)
    if [ -n "$CANDIDATE" ]; then
        JAR_FILE="$CANDIDATE"
    fi
fi

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "[DeluxeMenus Studio] Скомпилированный JAR-файл не найден. Запуск сборки..."
    if command -v mvn >/dev/null 2>&1; then
        mvn clean package -DskipTests
        JAR_FILE="$SCRIPT_DIR/target/deluxemenus-studio-1.3.1.jar"
    else
        echo "[DeluxeMenus Studio] Ошибка: Maven не найден для автоматической сборки." >&2
        exit 1
    fi
fi

echo "[DeluxeMenus Studio] Запуск $JAR_FILE..."
exec java -jar "$JAR_FILE" "$@"
