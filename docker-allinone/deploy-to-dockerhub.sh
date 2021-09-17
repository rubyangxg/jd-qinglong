#!/bin/bash
op=$1
if [ $op == 'push' ]; then
   echo "构建并推送镜像"
else
   echo "本地构建"
fi
docker rm -f jd1.1
docker rmi -f rubyangxg/jd-qinglong:1.1

cd ..
git pull --allow-unrelated-histories
mvn clean package -Dmaven.test.skip=true
cp target/jd-qinglong-*.jar docker-allinone
cd docker-allinone || exit
docker build -t rubyangxg/jd-qinglong:allinone --build-arg JAR_FILE=jd-qinglong-1.0.jar .
if [ $op == 'push' ]; then
   docker push rubyangxg/jd-qinglong:allinone
fi

docker run --name=jd-qinglong rubyangxg/jd-qinglong:allinone

#mvn clean package -Dmaven.test.skip=true && docker-compose -f docker-compose-debug.yml --env-file=env.properties  build --no-cache webapp
#docker-compose -f docker-compose-debug.yml --env-file=env.properties  up -d --no-deps && docker logs -f webapp