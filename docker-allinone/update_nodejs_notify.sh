#!/usr/bin/env bash

sudo git clone -b master -c http.sslVerify=false --depth=1 https://github.com/whyour/qinglong.git
sudo mkdir -p /opt/qinglong
sudo mkdir -p /opt/qinglong
cd qinglong/sample/ || exit
sudo npm config set strict-ssl false
sudo npm install
sudo cp -r /qinglong/sample /opt/qinglong/
sudo cp -r /qinglong/shell /opt/qinglong/
sudo rm -rf /qinglong
cd /opt/qinglong/shell || exit
sudo sed -i 's/\/ql\/scripts\/sendNotify.js/..\/sample\/notify.js/' /opt/qinglong/shell/notify.js