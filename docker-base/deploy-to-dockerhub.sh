#!/bin/bash
set -e

#当前目录
HOME=$(
  cd $(dirname $0)
  pwd
)

git pull

op=$1
if [[ $op == 'push' ]]; then
  echo "构建并推送镜像"
else
  op='local'
  echo "本地构建"
fi

docker build -t rubyangxg/selenium-base:latest .
if [[ $op == 'push' ]]; then
  docker login
  docker push rubyangxg/selenium-base:latest
fi