# 安装说明

## 前置要求

docker-compose更新至最新版：
```bash
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose  
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

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
#确保env.properties和docker-compose.yml处于同级目录
#根据需要修改配置文件内容
# 启动 -d表示后台静默启动
sudo docker-compose up -d
sudo docker-compose logs -f
```
## 更新

请直接pull最新的docker镜像即可

```bash
#停止阿东应用
docker-compose --env-file env.properties stop webapp
docker rm webapp && sudo docker rmi -f rubyangxg/jd-qinglong:allinone
#此操作不要轻易执行
#sudo docker-compose --env-file env.properties down
#下载最新阿东镜像
docker pull rubyangxg/jd-qinglong:allinone
docker-compose restart
docker-compose up -d
```