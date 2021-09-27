package com.meread.selenium.ws;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 相当于controller的处理器
 */
public class ApiHandler extends TextWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("=====ApiHandler接受到的数据" + payload);
        session.sendMessage(new TextMessage("ApiHandler返回收到的信息," + payload));
    }
}