#!/bin/sh
set -e
file=/go-cqhttp/go-cqhttp
if [ ! -f "$file" ]; then
  wget -O "go-cqhttp.tar.gz" https://ghproxy.com/https://github.com/Mrs4s/go-cqhttp/releases/download/v1.0.0-beta7-fix2/go-cqhttp_linux_amd64.tar.gz
  tar -zxvf go-cqhttp.tar.gz -C /go-cqhttp/
  chmod +x /go-cqhttp/*
  cp /config-template.yml /go-cqhttp/config.yml
else
  echo "go-cqhttp已存在"
fi

cd /go-cqhttp
./go-cqhttp