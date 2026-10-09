@echo off
setlocal EnableExtensions

rem DataHive launcher. Update these folders if you move Java, Maven, or Tomcat.
set "PROJECT_DIR=%~dp0"
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Java\jdk-27"
if not defined MAVEN_HOME set "MAVEN_HOME=C:\Users\shub8\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16"
if not defined CATALINA_HOME set "CATALINA_HOME=C:\Users\shub8\Downloads\apache-tomcat-10.1.60-windows-x64\apache-tomcat-10.1.60"

if "%~1"=="" goto menu
set "ACTION=%~1"
goto dispatch

:menu
echo.
echo  DataHive - local Tomcat control
echo  --------------------------------
echo  1. Build, deploy, and start
echo  2. Stop Tomcat
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

:checksetup
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo.
    echo Java was not found at:
    echo   %JAVA_HOME%
    echo Edit JAVA_HOME near the top of this file to point to your JDK folder.
    exit /b 1
)
if not exist "%CATALINA_HOME%\bin\startup.bat" (
    echo.
    echo Tomcat was not found at:
    echo   %CATALINA_HOME%
    echo Edit CATALINA_HOME near the top of this file to point to your Tomcat folder.
    exit /b 1
)
exit /b 0

:start
call :checksetup
if errorlevel 1 goto failed
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    where mvn.cmd >nul 2>nul
    if errorlevel 1 (
        echo.
        echo Maven was not found at:
        echo   %MAVEN_HOME%\bin\mvn.cmd
        echo Edit MAVEN_HOME near the top of this file to point to your Maven folder.
        goto failed
    )
    set "MAVEN_COMMAND=mvn.cmd"
) else (
    set "MAVEN_COMMAND=%MAVEN_HOME%\bin\mvn.cmd"
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo.
echo Building DataHive with Maven...
pushd "%PROJECT_DIR%"
call "%MAVEN_COMMAND%" -DskipTests clean package
if errorlevel 1 (
    popd
    echo.
    echo The build did not finish. Read the Maven message above, fix the issue, and run this file again.
    goto failed
)
if not exist "target\datahive.war" (
    popd
    echo.
    echo Maven finished but target\datahive.war was not created.
    goto failed
)

echo.
echo Stopping Tomcat before replacing the deployed app...
call "%CATALINA_HOME%\bin\shutdown.bat"
timeout /t 3 /nobreak >nul
echo Deploying DataHive to Tomcat...
copy /Y "target\datahive.war" "%CATALINA_HOME%\webapps\datahive.war" >nul
if errorlevel 1 (
    popd
    echo.
    echo Could not copy DataHive into Tomcat's webapps folder.
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
call :checksetup
if errorlevel 1 goto failed
echo Stopping Tomcat...
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
