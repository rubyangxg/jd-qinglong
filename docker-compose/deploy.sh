#!/bin/bash
set -e
detect_macos() {
    [[ $(uname -s) == Darwin ]] && is_macos=1 || is_macos=0
}

detect_macos

#当前目录
HOME=$(
  cd $(dirname $0)
  pwd
)

cd /root/docker-compose
docker stop webapp-selenoid && docker rm -f webapp-selenoid && docker rmi rubyangxg/jd-qinglong:1.3
docker-compose up