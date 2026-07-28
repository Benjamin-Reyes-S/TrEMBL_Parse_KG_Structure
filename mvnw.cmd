@echo off
setlocal EnableExtensions

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

set "BASE_DIR=%~dp0"
set "PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"
if not exist "%PROPERTIES%" (
    echo Missing %PROPERTIES% 1>&2
    exit /b 1
)

for /f "tokens=1,* delims==" %%A in (%PROPERTIES%) do (
    if "%%A"=="distributionUrl" set "DISTRIBUTION_URL=%%B"
)
set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MAVEN_BIN%" (
    echo Maven is not installed; downloading Maven %MAVEN_VERSION%... 1>&2
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ErrorActionPreference='Stop';" ^
      "$url='%DISTRIBUTION_URL%';" ^
      "$zip=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%-bin.zip';" ^
      "$stage=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%-wrapper';" ^
      "Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue;" ^
      "New-Item $stage -ItemType Directory -Force | Out-Null;" ^
      "Invoke-WebRequest $url -OutFile $zip;" ^
      "Expand-Archive $zip -DestinationPath $stage -Force;" ^
      "New-Item (Split-Path '%MAVEN_HOME%') -ItemType Directory -Force | Out-Null;" ^
      "Remove-Item '%MAVEN_HOME%' -Recurse -Force -ErrorAction SilentlyContinue;" ^
      "Move-Item (Join-Path $stage 'apache-maven-%MAVEN_VERSION%') '%MAVEN_HOME%';" ^
      "Remove-Item $stage -Recurse -Force; Remove-Item $zip -Force"
    if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
)

call "%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
