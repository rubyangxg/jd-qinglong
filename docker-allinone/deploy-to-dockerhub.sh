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

op=$1
if [[ $op == 'push' ]]; then
  echo "构建并推送镜像"
else
  op='local'
  echo "本地构建"
fi

if [ ! -f "$HOME/notify" ];then
  rm -rf qinglong && mkdir qinglong
  rm -rf notify
  git clone -b master --depth=1 https://github.com/whyour/qinglong.git
  cd qinglong/sample || exit

  if [[ $is_macos == 1 ]]; then
    sed -i '' 's/https:\/\/github.com\/whyour\/qinglong/https:\/\/github.com\/rubyangxg\/jd-qinglong/' notify.js
  else
    sed -i 's/https:\/\/github.com\/whyour\/qinglong/https:\/\/github.com\/rubyangxg\/jd-qinglong/' notify.js
  fi

  npm install
  cd ../shell || exit
  if [[ $is_macos == 1 ]]; then
    sed -i '' 's/\/ql\/scripts\/sendNotify.js/..\/sample\/notify.js/' notify.js
  else
    sed -i 's/\/ql\/scripts\/sendNotify.js/..\/sample\/notify.js/' notify.js
  fi

  pkg -t node16-linux-x64 notify.js
  cp notify $HOME/notify
else
  echo "notify已存在"
fi

#if [ ! -f "$HOME/jd_bean_change" ];then
#  rm -rf JD_tencent_scf && mkdir JD_tencent_scf
#  rm -rf jd_bean_change
#  git clone -b master --depth=1 https://github.com/zero205/JD_tencent_scf.git
#  npm install
#  pkg -t node16-linux-x64 jd_bean_change.js
#  cp jd_bean_change $HOME/jd_bean_change
#else
#  echo "jd_bean_change已存在"
#fi

cd $HOME || exit
rm -rf .npm
rm -rf .pkg-cache
rm -rf qinglong

docker rm -f webapp
docker rmi -f rubyangxg/jd-qinglong:latest
docker rmi -f rubyangxg/jd-qinglong:1.3

if [ ! -f "$HOME/jd-qinglong-1.0.jar" ];then
  cd ..
  git pull
  mvn clean package -Dmaven.test.skip=true
  cp target/jd-qinglong-*.jar docker-allinone
  cd docker-allinone || exit
else
  echo "jd-qinglong-1.0.jar已存在"
fi

docker build -t rubyangxg/jd-qinglong:latest --build-arg JAR_FILE=jd-qinglong-1.0.jar .
docker build -t rubyangxg/jd-qinglong:1.3 --build-arg JAR_FILE=jd-qinglong-1.0.jar .
if [[ $op == 'push' ]]; then
  docker login
  docker push rubyangxg/jd-qinglong:latest && docker push rubyangxg/jd-qinglong:1.3
fi

rm -rf $HOME/.docker
cd ..
docker stop webapp && docker rm webapp && docker rmi rubyangxg/jd-qinglong:1.3
#docker run -d -p 5701:8080 -p 4444:4444 -p 8001:8001 --name=webapp --privileged=true -e "SE_NODE_MAX_SESSIONS=8" -e "SPRING_PROFILES_ACTIVE=debugremote" -v /var/run/docker.sock:/var/run/docker.sock -v "$(pwd)"/env.properties:/env.properties:ro -v "$(pwd)"/go-cqhttp:/go-cqhttp rubyangxg/jd-qinglong:1.3

#mvn clean package -Dmaven.test.skip=true && docker-compose -f docker-compose-debug.yml --env-file=env.properties  build --no-cache webapp
#docker-compose -f docker-compose-debug.yml --env-file=env.properties  up -d --no-deps && docker logs -f webapp
