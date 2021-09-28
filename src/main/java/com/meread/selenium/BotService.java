package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.CommonAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.meread.selenium.JDService.strSpecialFilter;
import static com.meread.selenium.util.CommonAttributes.webSocketSession;

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
                sendMsgWithRetry(senderQQ, getNoChromeMsg());
            } else {
                sendMsgWithRetry(senderQQ, "正在准备发送验证码...");
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
                            sendMsgWithRetry(senderQQ, "已经获取到CK了，你的CK是" + ck);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                            sendMsgWithRetry(senderQQ, "请输入备注，以备注:(英文冒号)开头");
                            myChromeClient.setTrackCK(ck);
                        } else {
                            webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, "无法发送验证码")));
                        }
                        return;
                    }
                    boolean b = jdService.sendAuthCode(myChromeClient);
                    if (!b) {
                        log.warn(senderQQ + ", 屏幕状态:" + JSON.toJSONString(screen));
                        sendMsgWithRetry(senderQQ, "发送验证码失败");
                        return;
                    }
                    success = false;
                    Thread.sleep(1000);
                    retry = 0;
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
            sendMsgWithRetry(senderQQ, getNoChromeMsg());
        } else {
            if (myChromeClient.isExpire()) {
                sendMsgWithRetry(senderQQ, "超时了，请重新开始...");
                return;
            }
            sendMsgWithRetry(senderQQ, "正在准备登录...");
            threadPoolTaskExecutor.execute(() -> {
                jdService.controlChrome(myChromeClient, "sms_code", authCode);
                try {
                    boolean b = jdService.jdLogin(myChromeClient);
                    sendMsgWithRetry(senderQQ, b ? "已点击登录，请等待获取CK..." : "无法点击登录!请联系管理员");
                    if (b) {
                        int retry = 0;
                        boolean success = false;
                        while (retry++ < 10) {
                            JDCookie jdCookies = jdService.getJDCookies(myChromeClient);
                            if (!jdCookies.isEmpty()) {
                                myChromeClient.setTrackCK(jdCookies.toString());
                                webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, jdCookies.toString())));
                                sendMsgWithRetry(senderQQ, "请输入备注，以备注:(英文冒号)开头");
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

    public String getQLStatus(boolean select) {
        StringBuilder sb = new StringBuilder();
        List<QLConfig> qlConfigs = driverFactory.getQlConfigs();
        int size = qlConfigs.size();
        for (int i = 0; i < size; i++) {
            QLConfig ql = qlConfigs.get(i);
            sb.append(ql.getId()).append(". ").append(ql.getLabel()).append(":剩余容量->").append(ql.getRemain()).append(",登录方式->").append(ql.getQlLoginType().getDesc());
            if (i != size - 1) {
                sb.append("\n");
            }
        }
        if (select) {
            sb.append("\n例子：\n青龙:1(单个)\n青龙:234(多个)\n\n请输入，以青龙:(英文冒号)开头");
        }
        return sb.toString();
    }

    public void doUploadQinglong(long senderQQ, Set<Integer> chooseQLId) {
        WebSocketSession webSocketSession = CommonAttributes.webSocketSession;
        if (webSocketSession == null || !webSocketSession.isOpen()) {
            log.warn("webSocketSession not open");
            return;
        }
        threadPoolTaskExecutor.execute(() -> {
            MyChromeClient myChromeClient = driverFactory.getCacheMyChromeClient(String.valueOf(senderQQ));
            if (myChromeClient == null) {
                sendMsgWithRetry(senderQQ, getNoChromeMsg());
            } else {
                if (myChromeClient.isExpire()) {
                    sendMsgWithRetry(senderQQ, "超时了，请重新开始...");
                    return;
                }
                sendMsgWithRetry(senderQQ, "正在上传...");
                String phone = myChromeClient.getTrackPhone();
                if (StringUtils.isEmpty(phone)) {
                    sendMsgWithRetry(senderQQ, "你还没有输入手机号");
                    return;
                }
                String remark = myChromeClient.getTrackRemark();
                if (StringUtils.isEmpty(remark)) {
                    sendMsgWithRetry(senderQQ, "你还没有输入备注");
                    return;
                }
                String ck = myChromeClient.getTrackCK();
                if (StringUtils.isEmpty(ck)) {
                    sendMsgWithRetry(senderQQ, "你还没有获取到CK");
                    return;
                }
                int qlUploadDirect = 1;
                JSONObject jsonObject = jdService.uploadQingLong(chooseQLId, phone, remark, ck, myChromeClient.getChromeSessionId(), qlUploadDirect);
                String html = jsonObject.getString("html");
                log.info(jsonObject.toJSONString());
                if (!StringUtils.isEmpty(html)) {
                    html = html.replaceAll("<br/>", "\n");
                    sendMsgWithRetry(senderQQ, html);
                }
            }
        });
    }

    private String getNoChromeMsg() {
        StatClient statClient = driverFactory.getStatClient();
        int availChromeCount = statClient.getAvailChromeCount();
        if (availChromeCount > 0) {
            return "请重新开始吧！";
        }
        return "资源消耗殆尽，请稍后再试!\n" + "总资源数：" + statClient.getTotalChromeCount() + ", 可用资源个数：" + availChromeCount
                + "(网页占用：" + statClient.getWebSessionCount() + "，QQ占用：" + statClient.getQqSessionCount() + ")";
    }

    private void sendMsgWithRetry(long senderQQ, String content) {
        int retry = 0;
        while (retry++ <= 3) {
            try {
                webSocketSession.sendMessage(new TextMessage(buildPrivateMessage(senderQQ, content)));
                break;
            } catch (IOException e) {
                e.printStackTrace();
                log.info("与客户端qq : " + senderQQ + "通信失败 : ");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public void trackRemark(long senderQQ, String remark) {
        MyChromeClient myChromeClient = driverFactory.getCacheMyChromeClient(String.valueOf(senderQQ));
        if (myChromeClient == null) {
            sendMsgWithRetry(senderQQ, "请重新开始吧!");
        } else {
            if (myChromeClient.isExpire()) {
                sendMsgWithRetry(senderQQ, "超时了，请重新开始...");
                return;
            }
            remark = strSpecialFilter(remark);
            myChromeClient.setTrackRemark(StringUtils.isEmpty(remark) ? myChromeClient.getTrackPhone() : remark);
            sendMsgWithRetry(senderQQ, "设置备注成功!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            sendMsgWithRetry(senderQQ, getQLStatus(true));
        }
    }
}
