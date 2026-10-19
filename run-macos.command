#!/usr/bin/env bash
# DeluxeMenus Studio - macOS Launcher (.command)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== DeluxeMenus Studio (macOS) ==="

if ! command -v java >/dev/null 2>&1; then
    echo "[DeluxeMenus Studio] Ошибка: Java не установлена." >&2
    echo "Пожалуйста, установите Java 21 (Homebrew: brew install openjdk@21 или с сайта adoptium.net)." >&2
    read -n 1 -s -r -p "Нажмите любую клавишу для выхода..."
    exit 1
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
    echo "[DeluxeMenus Studio] JAR-файл не найден. Запуск компиляции через Maven..."
    if command -v mvn >/dev/null 2>&1; then
        mvn clean package -DskipTests
        JAR_FILE="$SCRIPT_DIR/target/deluxemenus-studio-1.3.1.jar"
    else
        echo "[DeluxeMenus Studio] Ошибка: Maven не найден для автоматической сборки." >&2
        read -n 1 -s -r -p "Нажмите любую клавишу для выхода..."
        exit 1
    fi
fi

echo "[DeluxeMenus Studio] Запуск приложения..."
exec java -Xdock:name="DeluxeMenus Studio" -jar "$JAR_FILE" "$@"
