### 安装前准备：

##### 以一台2h4g的Ubuntu Server 20.04 LTS 64bit为例：

1. 安装docker [官方教程](https://docs.docker.com/engine/install/ubuntu/)

```
sudo apt-get -y remove docker docker-engine docker.io containerd runc
sudo apt-get -y update
sudo apt-get -y install ca-certificates curl gnupg lsb-release
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
```

下面是一整行命令，全部复制

```
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
```

```
sudo apt-get update
sudo apt-get -y install docker-ce docker-ce-cli containerd.io
```

2. docker换源 执行以下命令，打开 /etc/docker/daemon.json 配置文件

```
vim /etc/docker/daemon.json
```

添加以下内容，并保存。

```
{
"registry-mirrors": [
"https://mirror.ccs.tencentyun.com"
]
}
```

3. 执行以下命令，重启 Docker 即可

```
sudo systemctl restart docker
```

4. 预下载依赖的镜像

```
docker pull selenoid/chrome:89.0
```

### 安装教程(暂不支持arm设备)：

1. 创建一个空目录（用于存放env.properties和go-cqhttp数据）

```
mkdir jd-qinglong && cd jd-qinglong
```

2. 下载配置文件模板，根据需要修改，不要缺少此文件

```
wget -O env.properties https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties
```

国内请使用：

```
wget -O env.properties https://ghproxy.com/https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties
```

3. 根据env.properties的注释，自行修改，**青龙密码登录方式不再支持，请换token方式登录**，具体在青龙-》系统设置-》应用设置-》添加应用
4. 安装nginx并配置

```
sudo apt-get -y install nginx
```

5. 新建nginx配置文件，用于novnc的websocket代理配置，**注意8082端口需开启外网访问**

```
vim /etc/nginx/conf.d/websocket.conf
```

添加以下内容：

```
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

upstream websocket {
    server localhost:4444; # appserver_ip:ws_port
    keepalive 20;
}

server {
    listen 8082;
    listen [::]:8082;
        location ~ /vnc/ {
        proxy_pass http://websocket;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }
    location ~ / {
        deny all;
    }
}
```

6. 重启nginx

```
service nginx restart
```

7. 下载rubyangxg/jd-qinglong镜像

```
sudo docker pull rubyangxg/jd-qinglong:1.6
```

8. 启动，其中env.properties中的SE_NODE_MAX_SESSIONS=8请根据机器配置改，一般一个chrome进程占用150M **_注意这是1条命令，全部复制执行_**

```
sudo docker run -d -p 5701:8080 -p 4444:4444 --name=webapp --privileged=true \ 
-v /var/run/docker.sock:/var/run/docker.sock \ 
-v [你的路径]/env.properties:/env.properties:rw \ 
-v [你的路径]/go-cqhttp:/go-cqhttp \
rubyangxg/jd-qinglong:1.6
```

例如：**_注意这是1条命令，全部复制执行_**

```
sudo docker run -d -p 5701:8080 -p 4444:4444 --name=webapp --privileged=true \
-v /var/run/docker.sock:/var/run/docker.sock \
-v "$(pwd)"/env.properties:/env.properties:ro \
-v "$(pwd)"/go-cqhttp:/go-cqhttp \
rubyangxg/jd-qinglong:1.6
``` 

或者编写docker-compose.yml

```
version: '3.3'
services:
    jd-qinglong:
        ports:
            - 5701:8080
            - 4444:4444
        container_name: jd-login
        privileged: true
        #environment: (失效，请在env.properties中配置资源数)
        #    - SE_NODE_MAX_SESSIONS=8
        volumes:
            - /var/run/docker.sock:/var/run/docker.sock
            - ./env.properties:/env.properties:ro
            - ./go-cqhttp:/go-cqhttp
        image: rubyangxg/jd-qinglong:1.6
```

然后在docker-compose.yml目录下执行命令

```
docker-compose up -d
```

9. 若要配置qq交互，往下看。
10. 启动后正常查看文件夹，应该是有一个名字叫go-cqhttp的目录。

```
root@VM-16-6-ubuntu:~/jd-qinglong# ls
env.properties  go-cqhttp
   ```

11. 查看go-cqhttp目录内容，看是否已经安装成功，确保文件大小类似这样

```
root@VM-16-6-ubuntu:~/jd-qinglong# ls -alh go-cqhttp/
total 16M
drwxr-xr-x 2 root       root 4.0K Sep 28 15:59 .
drwxr-xr-x 3 root       root 4.0K Sep 28 15:59 ..
-rw-r--r-- 1 root       root 3.5K Sep 28 15:59 config.yml
-rwxr-xr-x 1 lighthouse  121  16M Sep 17 19:08 go-cqhttp
-rwxr-xr-x 1 lighthouse  121  34K Sep 19 01:36 LICENSE
-rwxr-xr-x 1 lighthouse  121  20K Sep 19 01:36 README.md
```

12. 进入go-cqhttp目录：

```
root@VM-16-6-ubuntu:~/jd-qinglong# cd go-cqhttp/
```

13. 首次安装go-cqhttp后，直接运行，会提示：

```
root@VM-16-6-ubuntu:~/jd-qinglong/go-cqhttp# ./go-cqhttp
FATA[0000] 配置文件不合法!yaml: unmarshal errors:
  line 4: cannot unmarshal !!str `XXXXXXXXX` into int64 
```

14. 修改配置文件config.yml，填入你自己的qq账号密码，并保存修改后的config.yml：

```
  uin: XXXXXXXXX # qq号
  password: 'XXXXXXXXX' # qq密码
```

15. 再次执行，按照提示登录qq：

```
./go-cqhttp 
```

16. qq登录成功后，这里报错不用管，直接control+c退出，重启webapp：

```
docker restart webapp
```

17. **恭喜你安装成功。好用的话给我点个star吧！**

### 更新教程：

```
docker rm -f webapp
docker rmi rubyangxg/jd-qinglong:1.6
```

**上面两条命令执行完毕后，重新运行启动命令(安装教程第8步)**
