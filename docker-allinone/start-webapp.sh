#!/bin/sh
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8001 -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 /app.jar