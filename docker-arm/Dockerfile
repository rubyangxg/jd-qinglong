FROM arm64v8/openjdk:11-jdk

ENV TIME_ZONE=Asia/Shanghai

VOLUME /tmp
ARG JAR_FILE

COPY config-template.yml /

COPY ${JAR_FILE} app.jar
COPY notify /opt/bin/notify
#COPY jd_bean_change /opt/bin/jd_bean_change
COPY start-webapp.sh /opt/bin/start-webapp.sh
COPY start-go-cqhttp.sh /opt/bin/start-go-cqhttp.sh
#COPY gatgap.py /opt/bin/gatgap.py
#libopencv-dev python3-opencv

RUN apt-get update && apt-get install -y lsof tzdata ca-certificates tzdata mailcap supervisor curl chromium chromium-driver && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    chmod 755 /opt/bin/start-webapp.sh ; \
    chmod 755 /opt/bin/start-go-cqhttp.sh ; \
    chmod 755 /opt/bin/notify
#    chmod 755 /opt/bin/gatgap.py

EXPOSE 8080
COPY supervisord.conf /etc/supervisord.conf
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]