@echo off
title Renovacion Automatica Streamtape - CFilm
chcp 65001 > nul

echo ========================================================
echo   Iniciando Renovacion Automatica de Inactive Videos
echo ========================================================
echo.

set "PROJECT_DIR=C:\Users\Admin\AndroidStudioProjects\CFilm1"
set "PYTHON_CMD=python"

if exist "C:\Users\Admin\Inkscape\bin\python.exe" (
    set "PYTHON_CMD=C:\Users\Admin\Inkscape\bin\python.exe"
)

set "INACTIVOS_FILE="
if exist "%~dp0inactivos.txt" (
    set "INACTIVOS_FILE=%~dp0inactivos.txt"
) else if exist "%USERPROFILE%\Desktop\inactivos.txt" (
    set "INACTIVOS_FILE=%USERPROFILE%\Desktop\inactivos.txt"
) else if exist "%PROJECT_DIR%\inactivos.txt" (
    set "INACTIVOS_FILE=%PROJECT_DIR%\inactivos.txt"
)

if defined INACTIVOS_FILE (
    echo [*] Detectado archivo inactivos.txt en: %INACTIVOS_FILE%
    "%PYTHON_CMD%" "%PROJECT_DIR%\scripts\streamtape_remote_upload.py" --file "%INACTIVOS_FILE%" %*
) else (
    "%PYTHON_CMD%" "%PROJECT_DIR%\scripts\streamtape_remote_upload.py" --fetch-inactive %*
)

echo.
echo ========================================================
echo   Proceso finalizado.
echo ========================================================
pause
