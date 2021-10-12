FROM adoptopenjdk/openjdk11:alpine

ENV TZ=Asia/Shanghai
COPY MSYH.TTC /usr/share/fonts/
RUN echo "http://dl-cdn.alpinelinux.org/alpine/edge/main" > /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/v3.12/main" >> /etc/apk/repositories \
    && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ >> /etc/timezone \
    && apk add \
    libstdc++ \
    chromium \
    chromium-chromedriver \
    harfbuzz \
    supervisor \
    net-tools \
    lsof \
#    nss \
#    freetype \
#    ttf-freefont \
#    font-noto-emoji \
#    wqy-zenhei \
    && rm -rf /var/cache/* \
    && mkdir /var/cache/apk

VOLUME /tmp
EXPOSE 8080
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
