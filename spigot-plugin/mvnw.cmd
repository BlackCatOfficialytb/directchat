@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_PRELOAD%"=="" @echo off

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
if NOT "%JAVA_HOME%"=="" goto OkJHome

echo.
echo Error: JAVA_HOME is not set.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init

echo.
echo Error: JAVA_HOME is set to an invalid directory.
echo JAVA_HOME = "%JAVA_HOME%"
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.
echo.
goto error
@REM ==== END VALIDATION ====

:init
set MAVEN_CMD_LINE_ARGS=%*

@REM Find the project basedir
set MAVEN_PROJECTBASEDIR=%~dp0

@REM ==== DOWNLOAD AND RUN MAVEN ====
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"

@REM Set MAVEN_HOME to local directory
set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%.mvn\maven

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto runMaven

echo Downloading Maven...
mkdir "%MAVEN_HOME%" 2>nul
powershell -Command "& {Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%MAVEN_PROJECTBASEDIR%.mvn\maven.zip'}"
powershell -Command "& {Expand-Archive -Path '%MAVEN_PROJECTBASEDIR%.mvn\maven.zip' -DestinationPath '%MAVEN_PROJECTBASEDIR%.mvn' -Force}"
move "%MAVEN_PROJECTBASEDIR%.mvn\apache-maven-3.9.6" "%MAVEN_HOME%"
del "%MAVEN_PROJECTBASEDIR%.mvn\maven.zip"

:runMaven
set MAVEN_OPTS=-Xmx512m
"%MAVEN_HOME%\bin\mvn.cmd" %MAVEN_CMD_LINE_ARGS%

if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

exit /B %ERROR_CODE%
