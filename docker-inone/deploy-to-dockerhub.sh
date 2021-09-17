#!/bin/bash

docker rm -f jd1.1
docker rmi -f rubyangxg/jd-qinglong:1.1

cd ..
git pull --allow-unrelated-histories
mvn clean package -Dmaven.test.skip=true
cp target/jd-qinglong-*.jar docker-inone
cd docker-inone
docker build -t rubyangxg/jd-qinglong:1.1 --build-arg JAR_FILE=jd-qinglong-1.0.jar .

docker run --name=jd1.1 rubyangxg/jd-qinglong:1.1
#docker push rubyangxg/jd-qinglong:1.1

#mvn clean package -Dmaven.test.skip=true && docker-compose -f docker-compose-debug.yml --env-file=env.properties  build --no-cache webapp
#docker-compose -f docker-compose-debug.yml --env-file=env.properties  up -d --no-deps && docker logs -f webapp