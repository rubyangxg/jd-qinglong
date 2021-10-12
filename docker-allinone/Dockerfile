FROM rubyangxg/selenium-base
COPY config-template.yml /
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY notify /opt/bin/notify
#COPY jd_bean_change /opt/bin/jd_bean_change
COPY start-webapp.sh /opt/bin/start-webapp.sh
COPY start-go-cqhttp.sh /opt/bin/start-go-cqhttp.sh
RUN chmod 755 /opt/bin/start-webapp.sh && chmod 755 /opt/bin/start-go-cqhttp.sh && chmod 755 /opt/bin/notify
VOLUME /tmp
EXPOSE 8080
COPY supervisord.conf /etc/supervisord.conf
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]