### 安装教程：
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
3. 下载rubyangxg/jd-qinglong镜像，**_注意arm的镜像请用rubyangxg/jd-qinglong:arm_**
```
sudo docker pull rubyangxg/jd-qinglong
```
4. 启动，其中env.properties中的SE_NODE_MAX_SESSIONS=8请根据机器配置改，机器要求最少1h2g，推荐2h4g **_注意这是1条命令，全部复制执行_**
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true -v "$(pwd)"/env.properties:/env.properties:rw -v "$(pwd)"/adbot:/adbot rubyangxg/jd-qinglong
```
或者
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true --restart always \
-v [你的路径]/env.properties:/env.properties:rw \
-v [你的路径]/adbot:/adbot \
rubyangxg/jd-qinglong
```
arm的启动有所不同，请仔细甄别
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true --restart always \
-e "SPRING_PROFILES_ACTIVE=arm" \
-v [你的路径]/env.properties:/env.properties:rw \
-v [你的路径]/adbot:/adbot \
rubyangxg/jd-qinglong
```
例子：**_注意这是1条命令，全部复制执行，注意\后面不要有空格_**，
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true --restart always \
-v "$(pwd)"/env.properties:/env.properties:rw \
-v "$(pwd)"/adbot:/adbot \
rubyangxg/jd-qinglong
``` 
或者编写docker-compose.yml
```
version: '3.3'
services:
    jd-qinglong:
        ports:
            - 5701:8080
            - 8100:8100
        container_name: jd-login
        privileged: true
        volumes:
            - ./env.properties:/env.properties:rw
            - ./adbot:/adbot
        image: rubyangxg/jd-qinglong
```
然后在docker-compose.yml目录下执行命令
```
docker-compose up -d
```
5. 若要配置qq交互，往下看。
6. 使用：
    * env.properties中的ADBOT_QQ和ADBOT_PASSWORD必须配置，否则不能自动登录和识别机器人
    * 启动镜像后，请先访问8100，理论上会有一个待认证的机器人，你自己认证就行。如果没有，自行登录你的qq机器人（env.properties配置的那个），优先选择扫码登录，按照提示操作就行。
    * 登录成功后，重启镜像docker restart webapp
    * 如果碰到机器人假死，请执行 --> 重启 adbot
    
7. **恭喜你安装成功。好用的话给我点个star吧！**
### 更新教程：
#### 进入你的安装目录：
```
cd jd-qinglong
docker rm -f webapp && docker rmi rubyangxg/jd-qinglong && docker pull rubyangxg/jd-qinglong
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true -v "$(pwd)"/env.properties:/env.properties:rw -v "$(pwd)"/adbot:/adbot rubyangxg/jd-qinglong
```
**上面两条命令执行完毕后，重新运行启动命令(安装教程第4步)**
