package com.meread.selenium.ws;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 *{
 *     "font": 0,
 *     "message": [
 *         {
 *             "data": {
 *                 "text": "我是你"
 *             },
 *             "type": "text"
 *         }
 *     ],
 *     "message_id": -1419314522,
 *     "message_type": "private",
 *     "post_type": "message",
 *     "raw_message": "我是你",
 *     "self_id": 1904788864,
 *     "sender": {
 *         "age": 0,
 *         "nickname": "Jude",
 *         "sex": "unknown",
 *         "user_id": 87272738
 *     },
 *     "sub_type": "friend",
 *     "target_id": 1904788864,
 *     "time": 1632707571,
 *     "user_id": 87272738
 * }
 */
public class EventHandler extends TextWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("=====EventHandler接受到的数据" + payload);
        session.sendMessage(new TextMessage("EventHandler返回收到的信息," + payload));
    }
}