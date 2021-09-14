# 安装说明

## 前置要求

部署好[青龙面板](https://github.com/whyour/qinglong)

## 安装

1. 下载仓库中的`docker-compose.yml`至本地，或是复制文件内容后在本地自行建立并粘贴内容
2. 根据模板创建配置文件
3. 使用docker-compose启动
4. 打开浏览器使用

```bash
# 下载docker-compose.yml文件
wget https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/docker-compose.yml
# 下载配置文件模板
wget https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties
# 根据注释修改配置文件内容
cp env.template.properties env.properties
# 启动 -d表示后台静默启动
sudo docker-compose --env-file env.properties up -d
# 查看日志
sudo docker-compose --env-file env.properties logs -f
```

默认情况下jd-qinglong将会在`5701`端口启动，并将端口映射至容器所在宿主机，启动之后打开浏览器访问宿主机的5701端口即可（例如http://192.168.100.123:5701）。

## 更新

请直接pull最新的docker镜像即可

```bash
sudo docker-compose --env-file env.properties down
sudo docker pull rubyangxg/jd-qinglong:latest
sudo docker-compose --env-file env.properties up -d
```
