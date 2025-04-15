#!/bin/bash

# 设置字符集为 UTF-8
export LANG=en_US.UTF-8

# 检查是否提供了版本号参数
if [ -z "$1" ]; then
  echo "没有指定版本号，请使用 ./new-version.sh 1.0.0 格式调用"
  exit 1
fi

# 获取版本号
VERSION=$1

# 更新 openapi-generator-ui 版本号
echo "更新 openapi-generator-ui 版本号"
mvn versions:set -DnewVersion=$VERSION

# 打印成功信息
echo "执行版本更新成功：$VERSION"

exit 0
