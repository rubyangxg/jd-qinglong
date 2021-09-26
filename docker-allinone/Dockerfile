FROM adoptopenjdk/openjdk11:alpine

ENV TIME_ZONE=Asia/Shanghai

VOLUME /tmp
ARG JAR_FILE

COPY ${JAR_FILE} app.jar
COPY notify /opt/bin/notify
COPY selenoid /usr/bin
COPY start-webapp.sh /opt/bin/start-webapp.sh
COPY config /etc/selenoid
COPY go-cqhttp /go-cqhttp/

RUN apk add -U ca-certificates tzdata mailcap supervisor curl && rm -Rf /var/cache/apk/* && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/shanghai" >> /etc/timezone && \
    chmod 755 /opt/bin/start-webapp.sh && \
    chmod 755 /opt/bin/notify && \
    chmod 755 /usr/bin/selenoid
    chmod 755 /go-cqhttp/go-cqhttp

EXPOSE 4444 8080
COPY supervisord.conf /etc/supervisord.conf
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]