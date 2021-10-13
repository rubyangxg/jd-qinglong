#!/bin/bash
set -e
#当前目录
HOME=$(
  cd $(dirname $0)
  pwd
)
file=$HOME/jd-qinglong
if [ ! -f "$file" ]; then
  git clone https://ghproxy.com/https://github.com/rubyangxg/jd-qinglong.git
  cd jd-qinglong
else
  cd jd-qinglong
  echo "jd-qinglong已存在"
  git reset --hard
  git pull
fi
mvn clean package -Dmaven.test.skip=true
cp target/jd-qinglong-1.0.jar ../
cd $HOME
docker cp jd-qinglong-1.0.jar webapp-selenoid:/app.jar
docker restart webapp-selenoid
docker logs -f webapp-selenoid