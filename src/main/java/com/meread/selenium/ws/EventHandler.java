package com.meread.selenium.ws;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.BotService;
import com.meread.selenium.bean.qq.PrivateMessage;
import com.meread.selenium.util.CommonAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {
 * "font": 0,
 * "message": "登陆",
 * "message_id": 1744012386,
 * "message_type": "private",
 * "post_type": "message",
 * "raw_message": "登陆",
 * "self_id": 1904788864,
 * "sender": {
 * "age": 0,
 * "nickname": "Jude",
 * "sex": "unknown",
 * "user_id": 87272738
 * },
 * "sub_type": "friend",
 * "target_id": 1904788864,
 * "time": 1632709338,
 * "user_id": 87272738
 * }
 */
@Component
@Slf4j
public class EventHandler extends TextWebSocketHandler {

    @Autowired
    private BotService botService;

    @Value("${go-cqhttp.dir}")
    private String goCqHttpDir;

    private static final Pattern PATTERN = Pattern.compile("(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}");
    private static final Pattern PATTERN2 = Pattern.compile("\\d{6}");

    /**
     * socket 建立成功事件
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String webSocketSessionId = session.getId();
        log.info("afterConnectionEstablished " + webSocketSessionId);
        CommonAttributes.webSocketSession = session;
    }

    /**
     * socket 断开连接时
     *
     * @param session
     * @param status
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String webSocketSessionId = session.getId();
        log.info("afterConnectionClosed " + webSocketSessionId + ", CloseStatus" + status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (goCqHttpDir.endsWith("/")) {
            goCqHttpDir = goCqHttpDir.substring(0, goCqHttpDir.length() - 1);
        }
        File configPath = new File(goCqHttpDir + "/config.yml");
        if (!configPath.exists()) {
            log.warn(goCqHttpDir + "/config.yml文件不存在");
            return;
        }
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new FileSystemResource(configPath));
        Properties props = yamlFactory.getObject();
        String selfQQ = props.getProperty("account.uin", "0");

        String payload = message.getPayload();
        JSONObject jsonObject = JSON.parseObject(payload);
        String post_type = jsonObject.getString("post_type");
        if (!"message".equals(post_type)) {
            return;
        }
        log.info("payload = " + payload);
        //处理私聊消息
        PrivateMessage privateMessage = JSON.parseObject(payload, PrivateMessage.class);
        String content = privateMessage.getMessage();
        long senderQQ = privateMessage.getUser_id();
        long time = privateMessage.getTime();
        long target_id = privateMessage.getTarget_id();
        if (!selfQQ.equals(String.valueOf(target_id))) {
            log.info(goCqHttpDir + "/config.yml配置的qq号，不是此消息的接收人，接收人是" + target_id);
            return;
        }

        JSONObject jo = new JSONObject();
        jo.put("action", "send_private_msg");
        jo.put("echo", UUID.randomUUID().toString().replaceAll("-", ""));
        JSONObject params = new JSONObject();
        params.put("user_id", senderQQ);
        jo.put("params", params);

        Matcher matcher = PATTERN.matcher(content);
        Matcher matcher2 = PATTERN2.matcher(content);
        if ("登录".equals(content) || "登陆".equals(content)) {
            log.info("处理" + senderQQ + "登录逻辑...");
            params.put("message", "请输入手机号：");
        } else if (matcher.matches()) {
            log.info("处理给手机号" + content + "发验证码逻辑");
            params.put("message", "正在准备发送验证码...");
            botService.doSendSMS(senderQQ, content);
        } else if (matcher2.matches()) {
            log.info("接受了验证码" + content + "，处理登录逻辑");
            params.put("message", "正在处理登录...");
            botService.doLogin(senderQQ, content);
        } else if ("青龙状态".equals(content)) {
            log.info("接受了验证码" + content + "，处理登录逻辑");
            String qlStatus = botService.getQLStatus();
            params.put("message", qlStatus);
        }

        if (!StringUtils.isEmpty(params.getString("message"))) {
            session.sendMessage(new TextMessage(jo.toJSONString()));
        }
    }

}