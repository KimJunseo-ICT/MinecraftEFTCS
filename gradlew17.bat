@echo off
setlocal

set "JAVA_HOME=%USERPROFILE%\.jdks\temurin-17.0.21"
set "GRADLE_USER_HOME=%~dp0.gradle-user-home"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call "%~dp0gradlew.bat" %*
exit /b %ERRORLEVEL%
