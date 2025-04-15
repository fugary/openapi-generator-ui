#!/usr/bin/env bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
echo -e "Starting the @project.name@ ...\c"
JAVA_OPTS="-Xms512M -Xmx512M"
nohup java $JAVA_OPTS -jar $DEPLOY_DIR/@project.build.finalName@.jar >> /tmp/logs/@project.name@.log 2>&1 &
echo "Start Successfully ..."
