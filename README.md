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

<p align="center">
  <img width="49%" src="/public/images/home.png">
</p>

## 提醒一下
* arm的设备不用试了，没时间测试和解决遇到的问题
* 请选择性能好的设备运行，最好是云服务器或pc、x86软路由、群晖

## 如何安装
* [源码地址](https://github.com/rubyangxg/jd-qinglong)
* 已经安装好青龙的: [INSTALL1.md](INSTALL1.md)
* 从0搭建: [INSTALL2.md](INSTALL2.md)
* 免费代挂及问题反馈移步: [TG交流群](https://t.me/joinchat/3JfrwNPoHFY2MGNl)

## 多谢

* [青龙面板](https://github.com/whyour/qinglong)

* [docker-selenium](https://github.com/SeleniumHQ/docker-selenium)
## 常见问题
1. _Q: 为什么不能扫码登录？_  
**A: 扫码登录的ck有效期太短，已被阿东封禁，手机网页登录有效期1个月，目前没发现更好地方式**
2. _Q: 这是什么原理？_  
**A: 模拟一个后台的浏览器进行登录操作，想窥探究竟的自行阅读代码或者登录页链接加?debug=1（彩蛋）**
3. _Q: 页面上的可用资源数是什么意思？_  
**A: 由于每一个人的登录都要打开一个浏览器，比较消耗资源和内存，所以要限制打开浏览器的个数**
4. _Q: 为什么还有操作限时？_  
**A：如果长时间占用浏览器，不关闭，会导致浏览器资源迅速掏空，其他人就不能用了，所以操作时限3分钟强制释放占用的浏览器或者获取ck后强制释放**
5. _Q: 提示对不起，当前浏览器sessionId过期怎么办？_  
**A：隔几秒多刷几次就可以了，因为浏览器资源释放和创建需要时间**
