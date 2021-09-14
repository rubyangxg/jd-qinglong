#!/bin/bash
cd ..
git pull
mvn clean package -Dmaven.test.skip=true
cp target/jd-qinglong-*.jar docker/
cd docker
docker build -t rubyangxg/jd-qinglong:latest --build-arg JAR_FILE=jd-qinglong-1.0.jar .
docker push rubyangxg/jd-qinglong:latest
