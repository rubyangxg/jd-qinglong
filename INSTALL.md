### 安装教程：
1. 确保docker.sock存在(只要安装了docker一般都会有)
```
ls -alh /var/run/docker.sock
```
2. 创建一个空目录（用于存放env.properties和go-cqhttp数据）
```
mkdir jd-qinglong && cd jd-qinglong
```
3. 下载配置文件模板，根据需要修改，不要缺少此文件
```
wget -O env.properties https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties
```
国内请使用：
```
wget -O env.properties https://ghproxy.com/https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties
```
4. 下载chrome镜像（仅需执行一次，后续更新不用重新下载)
```
sudo docker pull selenoid/chrome:89.0
```
5. 下载rubyangxg/jd-qinglong镜像
```
sudo docker pull rubyangxg/jd-qinglong:1.2
```
6. 启动，其中SE_NODE_MAX_SESSIONS=8请根据机器配置改，**_注意这是1条命令，全部复制执行_**
```
sudo docker run -d -p 5701:8080 --name=webapp --privileged=true \ 
-e "SE_NODE_MAX_SESSIONS=8" \
-v /var/run/docker.sock:/var/run/docker.sock \ 
-v [你的路径]/env.properties:/env.properties:ro \ 
-v [你的路径]/go-cqhttp:/go-cqhttp \
rubyangxg/jd-qinglong:1.2
```
例如：**_注意这是1条命令，全部复制执行_**
```
sudo docker run -d -p 5701:8080 --name=webapp --privileged=true \
-e "SE_NODE_MAX_SESSIONS=8" \
-v /var/run/docker.sock:/var/run/docker.sock \
-v "$(pwd)"/env.properties:/env.properties:ro \
-v "$(pwd)"/go-cqhttp:/go-cqhttp \
rubyangxg/jd-qinglong:1.2
``` 
7. 若要配置qq交互，往下看。
8. 启动后正常查看文件夹，应该是有一个名字叫go-cqhttp的目录。
```
root@VM-16-6-ubuntu:~/jd-qinglong# ls
env.properties  go-cqhttp
   ```
9. 查看go-cqhttp目录内容，看是否已经安装成功，确保文件大小类似这样
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
10. 进入go-cqhttp目录：
```
root@VM-16-6-ubuntu:~/jd-qinglong# cd go-cqhttp/
```
11. 首次安装go-cqhttp后，直接运行，会提示：
```
root@VM-16-6-ubuntu:~/jd-qinglong/go-cqhttp# ./go-cqhttp
FATA[0000] 配置文件不合法!yaml: unmarshal errors:
  line 4: cannot unmarshal !!str `XXXXXXXXX` into int64 
```
12. 修改配置文件config.yml，填入你自己的qq账号密码，并保存修改后的config.yml：
```
  uin: XXXXXXXXX # qq号
  password: 'XXXXXXXXX' # qq密码
```
13. 再次执行，按照提示登录qq：
```
./go-cqhttp 
```
14. qq登录成功后，control+c退出，重启webapp：
```
docker restart webapp
```
15. **恭喜你安装成功。好用的话给我点个star吧！**
### 更新教程：
```
docker rm -f webapp
docker rmi rubyangxg/jd-qinglong:1.2
```
**上面两条命令执行完毕后，重新运行启动命令(安装教程第5步)**
