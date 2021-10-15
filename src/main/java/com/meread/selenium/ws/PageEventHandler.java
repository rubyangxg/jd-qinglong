package com.meread.selenium.ws;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.service.WSManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
@Slf4j
public class PageEventHandler extends TextWebSocketHandler {

    @Autowired
    private WSManager wsManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        wsManager.addNew(session);
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
        log.info("PageEventHandler close " + webSocketSessionId + ", CloseStatus" + status);
        wsManager.removeOld(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws IOException {
        String webSocketSessionId = session.getId();
        String payload = message.getPayload();
        JSONObject jsonObject = JSON.parseObject(payload);
        Boolean push = jsonObject.getBoolean("push");
        if (push != null && !push) {
            session.getAttributes().put("push", push);
        }
//        session.sendMessage(new TextMessage("Hi " + webSocketSessionId + " how may we help you?"));
        Long pingTime = jsonObject.getLong("ping");
    }

    // 错误处理（客户端突然关闭等接收到的错误）
    @Override
    public void handleTransportError(WebSocketSession arg0, Throwable arg1) throws Exception {
        if (arg0.isOpen()) {
            arg0.close();
        }
        arg1.printStackTrace();
        System.out.println("WS connection error,close...");
        wsManager.removeOld(arg0);
    }
}