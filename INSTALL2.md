# 安装说明

## 前置要求

需要安装`docker-ce`及`docker-compose`  
点击以下连接了解如何安装`docker`及`compose`

- [Debian](https://docs.docker.com/engine/install/debian/)
- [Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
- [CentOS](https://docs.docker.com/engine/install/centos/)
- [Fedora](https://docs.docker.com/engine/install/fedora/)
- [Red Hat Enterprise Linux](https://docs.docker.com/engine/install/rhel/)
- [SUSE Linux Enterprise Server](https://docs.docker.com/engine/install/sles/)

## 安装

1. 新建一个文件夹，用于存放相关数据
2. 下载仓库中的`docker-compose-allinone.yml`至本地，或是复制文件内容后在本地自行建立并粘贴内容
3. 使用docker-compose启动
4. 打开浏览器使用

```bash
# 新建数据文件夹
mkdir jd-qinglong
cd jd-qinglong
# 下载docker-compose.yml文件
wget -O docker-compose.yml https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/docker-compose-allinone.yml
# 下载配置文件模板
wget -O env.properties https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties
# 启动 -d表示后台静默启动
# 使用从0搭建的，不需要修改env.properties中的青龙配置
sudo docker-compose --env-file env.properties up -d
# 查看日志
sudo docker-compose --env-file env.properties logs -f
```
青龙面板默认端口5700
启动完毕后，会自动获取青龙的登录密码，密码查看命令：
```bash
cat data/config/auth.json
```

cat查看之后返回的结果类似如下字段

```json
{"username":"admin","password":"Xb-ZYP526wmg4_h6q1WqIO"}
```

输入此处记录的`username`及`password`，即可成功登陆qinglong面板，登陆后即可正常使用

## 备份

所有数据都将保存在`docker-compose.yml`所在的同级目录的`data`文件夹中，如需要备份，请直接备份`docker-compose.yml`及`data`文件夹即可

```bash
root@debian:/opt/qinglong# ls -lah
总用量 8.0K
drwxr-xr-x 3 root root 4.0K  8月 30 01:29 .
drwxr-xr-x 4 root root 4.0K  8月 30 00:51 ..
drwxr-xr-x 8 root root 4.0K  8月 30 01:30 data
-rw-r--r-- 1 root root  386  8月 30 01:29 docker-compose.yml
```

## 更新

请直接pull最新的docker镜像即可

```bash
cd jd-qinglong
#停止阿东应用
sudo docker-compose --env-file env.properties stop webapp
#此操作不要轻易执行
#sudo docker-compose --env-file env.properties down
#下载最新阿东镜像
sudo docker pull rubyangxg/jd-qinglong:latest
sudo docker-compose --env-file env.properties up -d
```

