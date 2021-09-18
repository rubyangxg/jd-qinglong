FROM selenium/standalone-chrome:4
ENV SE_NODE_SESSION_TIMEOUT=300
ENV SE_NODE_OVERRIDE_MAX_SESSIONS=true
ENV SE_NODE_MAX_SESSIONS=4
ENV START_XVFB=false
ENV TIME_ZONE=Asia/Shanghai
USER root

VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY selenium.conf /etc/supervisor/conf.d/selenium.conf
COPY start-webapp.sh /opt/bin/start-webapp.sh
COPY sources.list /etc/apt/sources.list
COPY notify /opt/bin/notify
RUN ln -snf /usr/share/zoneinfo/$TIME_ZONE /etc/localtime && echo $TIME_ZONE > /etc/timezone && \
    chmod 755 /opt/bin/start-webapp.sh && \
    chmod 755 /opt/bin/notify

USER seluser
EXPOSE 8080 5900
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]