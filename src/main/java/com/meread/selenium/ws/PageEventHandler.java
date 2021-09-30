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
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PageEventHandler extends TextWebSocketHandler {

    @Autowired
    private BotService botService;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException {
        String id = session.getId();
        String payload = message.getPayload();
        JSONObject jsonObject = JSON.parseObject(payload);
        session.sendMessage(new TextMessage("Hi " + id + " how may we help you?"));
    }
}