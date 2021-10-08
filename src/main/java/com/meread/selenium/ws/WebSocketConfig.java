package com.meread.selenium.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 服务端配置类
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private QQEventHandler qqEventHandler;

    @Autowired
    private PageEventHandler pageEventHandler;
    @Autowired
    private MyHandshakeInterceptor myHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(qqEventHandler, "ws/event").setAllowedOrigins("*");
        registry.addHandler(pageEventHandler, "ws/page/*").addInterceptors(myHandshakeInterceptor).setAllowedOrigins("*");
        registry.addHandler(pageEventHandler, "sockjs/ws/page/*").addInterceptors(myHandshakeInterceptor).setAllowedOrigins("*").withSockJS();
    }

}