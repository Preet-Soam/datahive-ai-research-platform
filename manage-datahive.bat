@echo off
setlocal EnableExtensions

set "PROJECT_DIR=%~dp0"
set "TOOLS_DIR=%PROJECT_DIR%.tools"
set "JAVA_HOME=%TOOLS_DIR%\jdk-21"
set "MAVEN_HOME=%TOOLS_DIR%\apache-maven-3.10.0"
set "CATALINA_HOME=%TOOLS_DIR%\apache-tomcat-10.1.60"
set "JRE_HOME=%JAVA_HOME%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%~1"=="" goto menu
set "ACTION=%~1"
goto dispatch

:menu
echo.
echo  DataHive - local setup
echo  ----------------------
echo  1. Set up, build, and run
echo  2. Stop DataHive
echo  3. Exit
echo.
choice /c 123 /n /m "Choose an option: "
if errorlevel 3 exit /b 0
if errorlevel 2 set "ACTION=stop"
if errorlevel 1 if not defined ACTION set "ACTION=start"

:dispatch
if /i "%ACTION%"=="start" goto start
if /i "%ACTION%"=="run" goto start
if /i "%ACTION%"=="stop" goto stop
echo Usage: %~nx0 [start^|stop]
exit /b 2

:start
echo.
echo Preparing the local Java, Maven, and Tomcat tools...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%setup-datahive.ps1"
if errorlevel 1 goto failed

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo.
    echo Java setup did not complete. Review the setup message above.
    goto failed
)
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Maven setup did not complete. Review the setup message above.
    goto failed
)
if not exist "%CATALINA_HOME%\bin\startup.bat" (
    echo Tomcat setup did not complete. Review the setup message above.
    goto failed
)

echo.
echo Building DataHive...
pushd "%PROJECT_DIR%"
call "%MAVEN_HOME%\bin\mvn.cmd" -DskipTests clean package
if errorlevel 1 (
    popd
    echo.
    echo The build failed. Review the Maven output above.
    goto failed
)
if not exist "target\datahive.war" (
    popd
    echo The build completed without creating target\datahive.war.
    goto failed
)

echo.
echo Stopping any previous DataHive Tomcat instance...
call "%CATALINA_HOME%\bin\shutdown.bat" >nul 2>&1
powershell.exe -NoProfile -Command "$deadline = (Get-Date).AddSeconds(15); do { $listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; if (-not $listener) { exit 0 }; Start-Sleep -Milliseconds 500 } while ((Get-Date) -lt $deadline); exit 1"
if errorlevel 1 (
    popd
    echo Port 8080 is still in use. Stop the other local server, then run this file again.
    goto failed
)
echo Deploying DataHive...
copy /Y "target\datahive.war" "%CATALINA_HOME%\webapps\datahive.war" >nul
if errorlevel 1 (
    popd
    echo Could not replace the deployed app. Close any other Tomcat using this folder and try again.
    goto failed
)
popd

echo Starting Tomcat...
call "%CATALINA_HOME%\bin\startup.bat"
if errorlevel 1 goto failed
echo.
echo DataHive is starting at http://localhost:8080/datahive/
goto success

:stop
if not exist "%CATALINA_HOME%\bin\shutdown.bat" (
    echo Tomcat has not been installed yet. Choose option 1 from manage-datahive.bat first.
    goto failed
)
echo Stopping DataHive...
call "%CATALINA_HOME%\bin\shutdown.bat"
if errorlevel 1 goto failed
echo Tomcat stop requested.
goto success

:success
if "%~1"=="" pause
exit /b 0

:failed
if "%~1"=="" pause
exit /b 1
