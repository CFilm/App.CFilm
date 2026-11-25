@echo off
title Renovacion Automatica Streamtape - CFilm
chcp 65001 > nul

echo ========================================================
echo   Iniciando Renovacion Automatica de Inactive Videos
echo ========================================================
echo.

cd /d "%~dp0"

if exist "C:\Users\Admin\Inkscape\bin\python.exe" (
    set "PYTHON_CMD=C:\Users\Admin\Inkscape\bin\python.exe"
) else (
    set "PYTHON_CMD=python"
)

"%PYTHON_CMD%" "%~dp0scripts\streamtape_remote_upload.py" --fetch-inactive %*

echo.
echo ========================================================
echo   Proceso finalizado.
echo ========================================================
pause
