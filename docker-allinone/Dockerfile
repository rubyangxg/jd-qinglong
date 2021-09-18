FROM selenium/standalone-chrome:4.0.0-rc-2-prerelease-20210915

ENV SE_NODE_SESSION_TIMEOUT=300
ENV SE_NODE_OVERRIDE_MAX_SESSIONS=true
ENV SE_NODE_MAX_SESSIONS=4
ENV START_XVFB=false

COPY sources.list /etc/apt/sources.list

USER root

#RUN apt-get update && \
#    apt-get -y install git-core curl nodejs npm

VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY selenium.conf /etc/supervisor/conf.d/selenium.conf
COPY start-webapp.sh /opt/bin/start-webapp.sh
COPY notify /opt/bin/notify
#COPY update_nodejs_notify.sh /opt/bin/update_nodejs_notify.sh
RUN chmod 755 /opt/bin/start-webapp.sh
RUN chmod 755 /opt/bin/notify
RUN #chmod 755 /opt/bin/update_nodejs_notify.sh
RUN #bash /opt/bin/update_nodejs_notify.sh

USER seluser
EXPOSE 8080 5900
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]