#!/bin/bash
echo "是否push：$1";
push=$1
cd ..
git pull --allow-unrelated-histories
mvn clean package -Dmaven.test.skip=true
cp target/jd-qinglong-*.jar docker/
cd docker
docker build -t rubyangxg/jd-qinglong:latest --build-arg JAR_FILE=jd-qinglong-1.0.jar .
if [ push == 1 ]; then
  docker push rubyangxg/jd-qinglong:latest
fi

#mvn clean package -Dmaven.test.skip=true && docker-compose -f docker-compose-debug.yml --env-file=env.properties  build --no-cache webapp
#docker-compose -f docker-compose-debug.yml --env-file=env.properties  up -d --no-deps && docker logs -f webapp