#!/bin/sh
# wait-for-grid.sh
url="http://localhost:4444/status"
code=`curl -I -m 30 -o /dev/null -s -w %{http_code}"\n" $url`           #第一次访问,访问成功则不进入下面while循环
while [ $code -ne 200 ]
do
  sleep 1s
  code=`curl -I -m 30 -o /dev/null -s -w %{http_code}"\n" $url`
done
#while循环访问url,直到状态码为200跳出循环
echo "开始启动webapp"
java -Djava.security.egd=file:/dev/./urandom -jar -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n -Dserver.port=8080 /app.jar