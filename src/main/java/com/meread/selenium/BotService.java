package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.AssignSessionIdStatus;
import com.meread.selenium.bean.JDCookie;
import com.meread.selenium.bean.JDScreenBean;
import com.meread.selenium.util.CommonAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;

/**
 * @author yangxg
 * @date 2021/9/26
 */
@Service
@Slf4j
public class BotService {

    @Autowired
    private WebDriverFactory driverFactory;

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
        //和网页获取不同，qq获取方式在获得到手机号以后才创建浏览器，所以create是true
        AssignSessionIdStatus status = driverFactory.assignSessionId(null, true, null, senderQQ);
        //请求一个sessionId
        log.info("doSendSMS : " + JSON.toJSONString(status));
        if (status.getAssignChromeSessionId() == null) {
            //分配sessionid失败
            //使前端垃圾cookie失效
            if (status.getClientChromeSessionId() != null) {
                log.info("doSendSMS pre release : " + JSON.toJSONString(status));
                driverFactory.releaseWebDriver(status.getClientChromeSessionId());
            }
            try {
                webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "会话失效，请重新输入!")));
            } catch (IOException e) {
                e.printStackTrace();
                log.info("与客户端qq : " + senderQQ + "通信失败");
            }
        } else {
            String assignChromeSessionId = status.getAssignChromeSessionId();
            if (status.isNew()) {
                driverFactory.bindSessionId(assignChromeSessionId);
            }
            threadPoolTaskExecutor.execute(() -> {
                try {
                    jdService.toJDlogin(assignChromeSessionId);
                    Thread.sleep(1000);
                    JDScreenBean screen = jdService.getScreen(assignChromeSessionId);
                    jdService.controlChrome(assignChromeSessionId, "phone", phone);

                    //STEP1
                    boolean success = false;
                    int retry = 0;
                    while (retry++ < 5) {
                        if (screen.isCanSendAuth()) {
                            success = true;
                            break;
                        }
                        Thread.sleep(1000);
                        screen = jdService.getScreen(assignChromeSessionId);
                    }
                    if (!success) {
                        webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "无法发送验证码")));
                        return;
                    }
                    boolean b = jdService.sendAuthCode(assignChromeSessionId);
                    Thread.sleep(1000);
                    log.info(senderQQ + ", 屏幕状态" + screen.getPageStatus() + "-->" + screen.getPageStatus().getDesc());
                    success = false;
                    while (retry++ < 5) {
                        if (screen.getPageStatus() == JDScreenBean.PageStatus.REQUIRE_VERIFY) {
                            webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "正在尝试第" + retry + "次滑块验证")));
                            jdService.crackCaptcha(assignChromeSessionId);
                            screen = jdService.getScreen(assignChromeSessionId);
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
            });
        }
    }

    public void doLogin(long senderQQ, String authCode) {
        WebSocketSession webSocketSession = CommonAttributes.webSocketSession;
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            log.warn("webSocketSession not open");
            return;
        }
        //和网页获取不同，qq获取方式在获得到手机号以后才创建浏览器，所以create是true
        AssignSessionIdStatus status = driverFactory.assignSessionId(null, false, null, senderQQ);
        //请求一个sessionId
        log.info("doSendSMS : " + JSON.toJSONString(status));
        if (status.getAssignChromeSessionId() == null) {
            //分配sessionid失败
            //使前端垃圾cookie失效
            if (status.getClientChromeSessionId() != null) {
                log.info("doSendSMS pre release : " + JSON.toJSONString(status));
                driverFactory.releaseWebDriver(status.getClientChromeSessionId());
            }
            try {
                webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "会话失效，请重新输入!")));
            } catch (IOException e) {
                e.printStackTrace();
                log.info("与客户端qq : " + senderQQ + "通信失败 : ");
            }
        } else {
            String assignChromeSessionId = status.getAssignChromeSessionId();
            if (status.isNew()) {
                driverFactory.bindSessionId(assignChromeSessionId);
            }
            threadPoolTaskExecutor.execute(() -> {
                jdService.controlChrome(assignChromeSessionId, "sms_code", authCode);
                try {
                    boolean b = jdService.jdLogin(assignChromeSessionId);
                    webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, b ? "已点击登录，请等待获取CK..." : "无法点击登录!请联系管理员")));
                    if (b) {
                        int retry = 0;
                        boolean success = false;
                        while (retry++ < 10) {
                            JDCookie jdCookies = jdService.getJDCookies(assignChromeSessionId);
                            if (!jdCookies.isEmpty()) {
                                webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, jdCookies.toString())));
                                success = true;
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
}
