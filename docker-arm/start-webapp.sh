#!/bin/sh
echo "开始启动webapp"
java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 /app.jar