### 本教程只适用于老版本升级过来的，需要改动的地方如下：
* 以防万一，**升级前请务必备份go-cqhttp目录中的ql.db到其他文件夹，go-cqhttp文件夹不要删**
* 以防万一，**升级前请务必备份go-cqhttp目录中的ql.db到其他文件夹，go-cqhttp文件夹不要删**
* 以防万一，**升级前请务必备份go-cqhttp目录中的ql.db到其他文件夹，go-cqhttp文件夹不要删**
    
1. 镜像版本号升级为1.9 **_注意arm的请把1.9替换为arm_**
```
sudo docker pull rubyangxg/jd-qinglong:1.9
```
2. 机器人新名字adbot(阿东机器人,阿这个字有点像狗)，env.properties必须增加如下设置：
```
#########adbot管理平台用户名密码，请务必改为自己的#########
AD_ADMIN_USERNAME=admin
AD_ADMIN_PASSWORD=adbotadmin
#####################

#########adbot(机器人qq)用户名密码#########
ADBOT_QQ=
ADBOT_QQ_PASSWORD=
#####################

#########adbot回复消息模式，0私聊，1群聊#########
ADBOT_REPLY_TYPE=0
#####################

#########青龙选择模式
# 0：显示青龙概要信息，让用户自己选择#########
# 1：自动上传所有青龙中容量最大的，容量相同的，按配置顺序#########
# 2：按配置顺序上传，满了则下一个#########
QL_CHOOSE_TYPE=2
#####################
```
3. 启动命令修改:
   * 增加了-p 8100:8100，左边的8100可自定义，需要开外网访问，用于网页上登录adbot，登录的用户名密码参见上方AD_ADMIN_USERNAME和AD_ADMIN_PASSWORD。不使用adbot的，无需映射8100端口
   * 挂载env.properties:ro改为env.properties:rw，用于之后做配置热生效。
   * 机器人实现替代~~go-cqhttp~~，统一为adbot，启动命令**_不要挂载go-cqhttp_**, 请仔细甄别。
   * 如果复制出来的命令\后面有空格，请去掉
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true -v [你的路径]/env.properties:/env.properties:rw -v [你的路径]/adbot:/adbot rubyangxg/jd-qinglong:1.9
```
或者
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true --restart always \
-v [你的路径]/env.properties:/env.properties:rw \
-v [你的路径]/adbot:/adbot \
rubyangxg/jd-qinglong:1.9
```
arm的启动有所不同，请仔细甄别
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true --restart always \
-e "SPRING_PROFILES_ACTIVE=arm" \
-v [你的路径]/env.properties:/env.properties:rw \
-v [你的路径]/adbot:/adbot \
rubyangxg/jd-qinglong:1.9
```
例如：**_注意这是1条命令，全部复制执行，注意\后面不要有空格_**，
```
sudo docker run -d -p 5701:8080 -p 8100:8100 --name=webapp --privileged=true --restart always \
-v "$(pwd)"/env.properties:/env.properties:rw \
-v "$(pwd)"/adbot:/adbot \
rubyangxg/jd-qinglong:1.9
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
        image: rubyangxg/jd-qinglong:1.9
```
然后在docker-compose.yml目录下执行命令
```
docker-compose up -d
```
4. 使用：
   * 启动镜像后，请先访问8100，理论上会有一个待认证的机器人，你自己认证就行。如果没有，自行登录你的qq机器人（env.properties配置的那个），优先选择扫码登录，按照提示操作就行。
   * 登录成功后，重启镜像docker restart webapp
   * 如果碰到机器人假死，请执行 --> 重启 adbot 
    
5. **恭喜你安装成功。好用的话给我点个star吧！**
### 更新教程：
```
docker rm -f webapp
docker rmi rubyangxg/jd-qinglong:1.9
```
```
docker exec -it webapp guide
```
**上面两条命令执行完毕后，重新运行启动命令(安装教程第4步)**
