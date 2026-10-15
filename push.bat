@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo Отправка проекта на GitHub...
git push -u origin main
echo.
echo Готово! Нажмите любую клавишу, чтобы закрыть это окно...
pause
