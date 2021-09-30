package com.meread.selenium.ws;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

/**
 * 类描述：拦截器
 * @author yangxg
 */
@Component
@Slf4j
public class MyHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception ex) {
        log.info("After handshake " + request.getRemoteAddress());
        super.afterHandshake(request, response, wsHandler, ex);
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                                   Map<String, Object> map) throws Exception {
        log.info("Before handshake " + request.getRemoteAddress());
        return super.beforeHandshake(request, response, handler, map);
    }

}  