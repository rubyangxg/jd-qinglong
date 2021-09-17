FROM selenium/standalone-chrome:4.0.0-rc-2-prerelease-20210915
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY selenium.conf /etc/supervisor/conf.d/selenium.conf
COPY start-webapp.sh /opt/bin/start-webapp.sh
USER root
RUN chmod 755 /opt/bin/start-webapp.sh
USER seluser
EXPOSE 8080 4444 5555 7900 5900
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]