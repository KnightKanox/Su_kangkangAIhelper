@echo off
setlocal
cd /d "%~dp0"

where py >nul 2>nul
if %errorlevel%==0 (
    py -3 -m venv .venv
) else (
    python -m venv .venv
)
if errorlevel 1 goto fail

call .venv\Scripts\activate.bat
if errorlevel 1 goto fail
python -m pip install --upgrade pyinstaller pygame-ce==2.5.8
if errorlevel 1 goto fail
python -m PyInstaller --noconfirm --clean --onefile --windowed --name KangKangShiYangAI --icon "assets\app.ico" --add-data "assets;assets" --add-data "data\recipes.csv;data" app.py
if errorlevel 1 goto fail

echo Build succeeded: dist\KangKangShiYangAI.exe
pause
exit /b 0

:fail
echo Build failed. Check the error above.
pause
exit /b 1
