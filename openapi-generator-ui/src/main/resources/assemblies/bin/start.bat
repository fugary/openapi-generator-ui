@echo off
chcp 65001
cd ..
echo current jdk: %JAVA_HOME%
if "%JDK21%" equ "" goto startApplication
echo JDK21 exists: %JDK21%
set JAVA_HOME=%JDK21%
set PATH=%JAVA_HOME%\bin;%PATH%
:startApplication
title @project.name@
echo Starting the @project.name@ ...
set JAVA_OPTS=-Xms512M -Xmx512M
java %JAVA_OPTS% -jar @project.build.finalName@.jar
