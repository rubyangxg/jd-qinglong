FROM adoptopenjdk/openjdk11:latest
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8080
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]