package com.meread.selenium.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 服务端配置类
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(apiHandler(), "api").setAllowedOrigins("*");
        registry.addHandler(eventHandler(), "event").setAllowedOrigins("*");
    }

    public WebSocketHandler apiHandler() {
        return new ApiHandler();
    }

    public WebSocketHandler eventHandler() {
        return new EventHandler();
    }

}