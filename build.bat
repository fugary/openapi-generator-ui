@echo off
echo current jdk: %JAVA_HOME%
if "%JDK21%" equ "" goto buildStart
echo JDK21 exists: %JDK21%
set JAVA_HOME=%JDK21%
set PATH=%JAVA_HOME%\bin;%PATH%
:buildStart
echo use jdk: %JAVA_HOME%
set BUILD_ARGS=-Dmaven.test.skip=true -T 1C -Dmaven.compile.fork=true
echo "===========================build==========================="
start mvn clean install %BUILD_ARGS%
