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
3. 下载rubyangxg/jd-qinglong镜像，**_注意arm的请把1.7替换为arm_**
```
sudo docker pull rubyangxg/jd-qinglong:1.7
```
4. 启动，其中env.properties中的SE_NODE_MAX_SESSIONS=8请根据机器配置改，一般一个chrome进程占用150M **_注意这是1条命令，全部复制执行，注意\后面不要有空格_**
```
sudo docker run -d -p 5701:8080 --name=webapp --privileged=true \ 
-v [你的路径]/env.properties:/env.properties:ro \ 
-v [你的路径]/go-cqhttp:/go-cqhttp \
rubyangxg/jd-qinglong:1.7
```
**_注意arm的用这个_**
```
sudo docker run -d -p 5701:8080 --name=webapp --privileged=true \
-e "SPRING_PROFILES_ACTIVE=arm" \
-v [你的路径]/env.properties:/env.properties:ro \
-v [你的路径]/go-cqhttp:/go-cqhttp \
rubyangxg/jd-qinglong:1.7
```
例如：**_注意这是1条命令，全部复制执行，注意\后面不要有空格_**，
```
sudo docker run -d -p 5701:8080 --name=webapp --privileged=true \
-v "$(pwd)"/env.properties:/env.properties:ro \
-v "$(pwd)"/go-cqhttp:/go-cqhttp \
rubyangxg/jd-qinglong:1.7
``` 
或者编写docker-compose.yml
```
version: '3.3'
services:
    jd-qinglong:
        ports:
            - 5701:8080
        container_name: jd-login
        privileged: true
        #environment: (失效，请在env.properties中配置资源数)
        #    - SE_NODE_MAX_SESSIONS=8
        volumes:
            - ./env.properties:/env.properties:ro
            - ./go-cqhttp:/go-cqhttp
        image: rubyangxg/jd-qinglong:1.7
```
然后在docker-compose.yml目录下执行命令
```
docker-compose up -d
```
5. 若要配置qq交互，往下看。
6. 启动后正常查看文件夹，应该是有一个名字叫go-cqhttp的目录。
```
root@VM-16-6-ubuntu:~/jd-qinglong# ls
env.properties  go-cqhttp
   ```
7. 查看go-cqhttp目录内容，看是否已经安装成功，确保文件大小类似这样
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
8. 进入go-cqhttp目录：
```
root@VM-16-6-ubuntu:~/jd-qinglong# cd go-cqhttp/
```
9. 首次安装go-cqhttp后，直接运行，会提示：
```
root@VM-16-6-ubuntu:~/jd-qinglong/go-cqhttp# ./go-cqhttp
FATA[0000] 配置文件不合法!yaml: unmarshal errors:
  line 4: cannot unmarshal !!str `XXXXXXXXX` into int64 
```
10. 修改配置文件config.yml，填入你自己的qq账号密码，并保存修改后的config.yml：
```
  uin: XXXXXXXXX # qq号
  password: 'XXXXXXXXX' # qq密码
```
11. 再次执行，按照提示登录qq：
```
./go-cqhttp 
```
12. qq登录成功后，这里报错不用管，直接control+c退出，重启webapp：
```
docker restart webapp
```
13. **恭喜你安装成功。好用的话给我点个star吧！**
### 更新教程：
```
docker rm -f webapp
docker rmi rubyangxg/jd-qinglong:1.7
```
**上面两条命令执行完毕后，重新运行启动命令(安装教程第4步)**
