FROM alpine:latest

ENV JAVA_HOME="/usr/lib/jvm/default-jvm/"
ENV TZ=Asia/Shanghai

COPY config-template.yml /
COPY ${JAR_FILE} app.jar
COPY notify /opt/bin/notify
#COPY jd_bean_change /opt/bin/jd_bean_change
COPY start-webapp.sh /opt/bin/start-webapp.sh
COPY start-go-cqhttp.sh /opt/bin/start-go-cqhttp.sh
#COPY MSYH.TTC /usr/share/fonts/

# Installs latest Chromium package.
RUN echo "http://dl-cdn.alpinelinux.org/alpine/edge/main" > /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/v3.12/main" >> /etc/apk/repositories \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ >> /etc/timezone \
    && apk upgrade -U -a \
    && apk add \
    openjdk11 \
    libstdc++ \
    chromium \
    chromium-chromedriver \
    harfbuzz \
    net-tools \
    lsof \
    nss \
    freetype \
    ttf-freefont \
    font-noto-emoji \
    wqy-zenhei \
    && rm -rf /var/cache/* \
    && mkdir /var/cache/apk \
    && chmod 755 /opt/bin/start-webapp.sh \
    && chmod 755 /opt/bin/start-go-cqhttp.sh \
    && chmod 755 /opt/bin/notify

COPY local.conf /etc/fonts/local.conf
ENV PATH=$PATH:${JAVA_HOME}/bin
# Add Chrome as a user
RUN mkdir -p /usr/src/app \
    && adduser -D chrome \
    && chown -R chrome:chrome /usr/src/app
# Run Chrome as non-privileged
USER chrome
WORKDIR /usr/src/app

ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

VOLUME /tmp
EXPOSE 8080
COPY supervisord.conf /etc/supervisord.conf
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
#JAVA_OPTS="-server -Xmx2g -Xms2g -Xss512k -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom "
#DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n "
#ENTRYPOINT [ "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar -Dserver.port=8080 app.jar"]