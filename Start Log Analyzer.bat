@echo off
setlocal
cd /d "%~dp0"
if not exist "target\log-analyzer-1.0.0.jar" (
  echo Please run Build and Test.bat first.
  pause
  exit /b 1
)
set "JAVA_EXE="
if defined JAVA_HOME call :tryJava "%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE call :tryJava "java"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do call :tryJava "%%~D\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\Java\jdk-*") do call :tryJava "%%~D\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\Microsoft\jdk-*") do call :tryJava "%%~D\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\JetBrains\IntelliJ IDEA*") do call :tryJava "%%~D\jbr\bin\java.exe"
if not defined JAVA_EXE (
  echo Java 25 or newer is required. See docs\USER_GUIDE.md for setup steps.
  pause
  exit /b 1
)
"%JAVA_EXE%" -jar "target\log-analyzer-1.0.0.jar" --gui
if errorlevel 1 pause
exit /b
:tryJava
if defined JAVA_EXE exit /b
"%~1" -version >nul 2>&1
if errorlevel 1 exit /b
"%~1" -version 2>&1 | findstr /R /C:"version \"25\." >nul
if errorlevel 1 exit /b
"%~1" --dry-run -jar "target\log-analyzer-1.0.0.jar" >nul 2>&1
if not errorlevel 1 set "JAVA_EXE=%~1"
exit /b
