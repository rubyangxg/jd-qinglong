## **功能：**
1. 手机验证码获取X东CK，CK有效期默认一个月 
2. qq扫码获取X东CK，CK有效期默认一个月 
3. 对接青龙面板，多青龙选择上传
4. 对接xdd-plus

<p align="center">
  用于替代Ninja【代码仅供参考学习，禁止商用和非法用途，否则，一切后果请用户自负。】
</p>

<div align="center">

[![docker version][docker-version-image]][docker-version-url] [![docker pulls][docker-pulls-image]][docker-pulls-url] [![docker stars][docker-stars-image]][docker-stars-url] [![docker image size][docker-image-size-image]][docker-image-size-url] 

[docker-pulls-image]: https://img.shields.io/docker/pulls/rubyangxg/jd-qinglong?style=flat
[docker-pulls-url]: https://hub.docker.com/r/rubyangxg/jd-qinglong
[docker-version-image]: https://img.shields.io/docker/v/rubyangxg/jd-qinglong?style=flat
[docker-version-url]: https://hub.docker.com/r/rubyangxg/jd-qinglong/tags?page=1&ordering=last_updated
[docker-stars-image]: https://img.shields.io/docker/stars/rubyangxg/jd-qinglong?style=flat
[docker-stars-url]: https://hub.docker.com/r/rubyangxg/jd-qinglong
[docker-image-size-image]: https://img.shields.io/docker/image-size/rubyangxg/jd-qinglong?style=flat
[docker-image-size-url]: https://hub.docker.com/r/rubyangxg/jd-qinglong
</div>

## 提醒一下
* 请选择性能好的设备运行，最好是云服务器或pc、x86软路由、oracle-arm(4-24)、群晖。
* 只支持青龙2.9+，2.8可能会出现无法上传问题。
* 本项目可以不依赖青龙运行，获取的CK需网页上手工复制

## 更新历史
* 2021-10-10 1.3版本新增支持arm，只在oracle-arm(4-24)测试通过。回归原始chromedriver，不依赖selenoid等其他镜像，无需挂载/var/run/docker.sock
* 2021-10-08 修复已知bug，修复资源回收慢问题，采用新的ws协议与chrome交互，速度更快，增加扫码登录，增加bot监控群聊
* 2021-09-28 增加qq面板交互，自定义标题，自定义公告栏吗，重构代码
* 2021-09-25 解决配置资源数低，或其他情况下启动后显示无青龙配置bug，如果都正常就不需要更新。更新命令看[INSTALL.md](INSTALL.md)
* 2021-09-21 优化资源回收创建流程，优化资源404时间过长
* 2021-09-18 新增推送功能，配置同青龙，请查看[env.template.properties](https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties)

[comment]: <> (* 2021-09-17 解决验证码输入后登录按钮无效问题，请升级成allinone镜像)

[comment]: <> (* 2021-09-17 推出allinone镜像，无需其他依赖，升级的话只修改docker-compose.yml即可)

[comment]: <> (* 2021-09-16 bug fix 解决了卡验证码问题，请务必pull最新镜像)

[comment]: <> (*            增加实验功能：支持配置最多上传ck容量)

[comment]: <> (* 2021-09-15 更新上传多青龙支持，最多5个，升级后请仔细阅读 [env.template.properties]&#40;https://raw.githubusercontent.com/rubyangxg/jd-qinglong/master/env.template.properties&#41; 里面的注释)

## 如何安装
* [源码地址](https://github.com/rubyangxg/jd-qinglong)
* **群晖**安装参考源码路径下的 **jd-qinglong-群晖安装教程.pdf**
* 新版本安装教程: [INSTALL.md](INSTALL.md)
* 问题反馈移步: [TG交流群](https://t.me/joinchat/3JfrwNPoHFY2MGNl)

## 多谢

* [青龙面板](https://github.com/whyour/qinglong)

* [docker-selenium](https://github.com/SeleniumHQ/docker-selenium)
## 支持
如果觉得我的项目对你小有帮助
那么下面的微信赞赏码可以扫一扫啦：
<img src="https://github.com/rubyangxg/jd-qinglong/raw/master/donate/Wechat.png" width="400" height="400" alt="微信小程序"/><br/>
