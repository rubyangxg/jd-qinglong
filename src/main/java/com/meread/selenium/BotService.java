package com.meread.selenium;

import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.CommonAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * @author yangxg
 * @date 2021/9/26
 */
@Service
@Slf4j
public class BotService {

    @Autowired
    private WebDriverManager driverFactory;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Autowired
    private JDService jdService;

    public void doSendSMS(long senderQQ, String phone) {
        WebSocketSession webSocketSession = CommonAttributes.webSocketSession;
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            log.warn("webSocketSession not open");
            return;
        }
        threadPoolTaskExecutor.execute(() -> {
            //和网页获取不同，qq获取方式在获得到手机号以后才创建浏览器，所以create是true
            MyChromeClient myChromeClient = driverFactory.getCacheMyChromeClient(String.valueOf(senderQQ));
            if (myChromeClient == null) {
                myChromeClient = driverFactory.createNewMyChromeClient(String.valueOf(senderQQ), LoginType.QQBOT, JDLoginType.phone);
            }
            if (myChromeClient == null) {
                try {
                    webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "资源消耗殆尽，请稍后再试!")));
                } catch (IOException e) {
                    e.printStackTrace();
                    log.info("与客户端qq : " + senderQQ + "通信失败");
                }
            } else {
                try {
                    myChromeClient.setTrackQQ(senderQQ);
                    myChromeClient.setTrackPhone(phone);
                    jdService.toJDlogin(myChromeClient);
                    Thread.sleep(1000);
                    JDScreenBean screen = jdService.getScreen(myChromeClient);
                    jdService.controlChrome(myChromeClient, "phone", phone);

                    //STEP1
                    boolean success = false;
                    int retry = 0;
                    while (retry++ < 5) {
                        if (screen.isCanSendAuth()) {
                            success = true;
                            break;
                        }
                        Thread.sleep(1000);
                        screen = jdService.getScreen(myChromeClient);
                    }
                    if (!success) {
                        if (screen.getPageStatus() == JDScreenBean.PageStatus.SUCCESS_CK) {
                            String ck = screen.getCk().toString();
                            webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "已经获取到CK了，你的CK是" + ck)));
                            myChromeClient.setTrackCK(ck);
                        } else {
                            webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "无法发送验证码")));
                        }
                        return;
                    }
                    boolean b = jdService.sendAuthCode(myChromeClient);
                    Thread.sleep(1000);
                    log.info(senderQQ + ", 屏幕状态" + screen.getPageStatus() + "-->" + screen.getPageStatus().getDesc());
                    success = false;
                    while (retry++ < 5) {
                        if (screen.getPageStatus() == JDScreenBean.PageStatus.REQUIRE_VERIFY) {
                            webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "正在尝试第" + retry + "次滑块验证")));
                            jdService.crackCaptcha(myChromeClient);
                            screen = jdService.getScreen(myChromeClient);
                            if (screen.getPageStatus() != JDScreenBean.PageStatus.REQUIRE_VERIFY) {
                                success = true;
                                break;
                            }
                        } else {
                            success = true;
                        }
                        Thread.sleep(1000);
                    }
                    if (success) {
                        webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, b ? "已发送验证码，请注意查收!" : "无法发送验证码!")));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("与客户端qq : " + senderQQ + "通信失败");
                }
            }
        });
    }

    public void doLogin(long senderQQ, String authCode) {
        WebSocketSession webSocketSession = CommonAttributes.webSocketSession;
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            log.warn("webSocketSession not open");
            return;
        }
        //和网页获取不同，qq获取方式在获得到手机号以后才创建浏览器，所以create是true
        MyChromeClient myChromeClient = driverFactory.getCacheMyChromeClient(String.valueOf(senderQQ));
        if (myChromeClient == null) {
            try {
                webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "资源消耗殆尽，请稍后再试!")));
            } catch (IOException e) {
                e.printStackTrace();
                log.info("与客户端qq : " + senderQQ + "通信失败 : ");
            }
        } else {
            threadPoolTaskExecutor.execute(() -> {
                jdService.controlChrome(myChromeClient, "sms_code", authCode);
                try {
                    boolean b = jdService.jdLogin(myChromeClient);
                    webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, b ? "已点击登录，请等待获取CK..." : "无法点击登录!请联系管理员")));
                    if (b) {
                        int retry = 0;
                        boolean success = false;
                        while (retry++ < 10) {
                            JDCookie jdCookies = jdService.getJDCookies(myChromeClient);
                            if (!jdCookies.isEmpty()) {
                                myChromeClient.setTrackCK(jdCookies.toString());
                                webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, jdCookies.toString())));
                                success = true;
                                driverFactory.releaseWebDriver(myChromeClient.getChromeSessionId());
                                break;
                            }
                            Thread.sleep(1000);
                        }
                        if (!success) {
                            webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "获取ck失败！")));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.info("与客户端qq : " + senderQQ + "通信失败 : ");
                }
            });
        }
    }

    private String buildPrivateMessage(long receiverQQ, String content) {
        JSONObject jo = new JSONObject();
        jo.put("action", "send_private_msg");
        jo.put("echo", UUID.randomUUID().toString().replaceAll("-", ""));
        JSONObject params = new JSONObject();
        params.put("user_id", receiverQQ);
        params.put("message", content);
        jo.put("params", params);
        return jo.toJSONString();
    }

    public String getQLStatus() {
        StringBuilder sb = new StringBuilder();
        List<QLConfig> qlConfigs = driverFactory.getQlConfigs();
        for (QLConfig ql : qlConfigs) {
            sb.append(ql.getLabel() + ":剩余容量" + ql.getRemain() + ",登录方式" + ql.getQlLoginType().getDesc() + "\n");
        }
        return sb.toString();
    }
}
