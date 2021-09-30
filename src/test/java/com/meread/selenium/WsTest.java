package com.meread.selenium;

import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Containers;
import com.amihaiemil.docker.UnixDocker;
import com.meread.selenium.ws.PageEventHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yangxg on 2021/9/22
 *
 * @author yangxg
 */
public class WsTest {
    public static void main(String[] args) throws IOException {
//        curl --include \
//        --no-buffer \
//        --header "Connection: Upgrade" \
//        --header "Upgrade: websocket" \
//        --header "Host: localhost:6700" \
//        --header "Origin: http://localhost:6700" \
//        --header "Sec-WebSocket-Key: XXXXXXXXX" \
//        --header "Sec-WebSocket-Version: 13" \
//        http://localhost:6700/
        List<Transport> transports = new ArrayList<>(2);
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        transports.add(new RestTemplateXhrTransport());

        SockJsClient sockJsClient = new SockJsClient(transports);
        sockJsClient.doHandshake(new PageEventHandler(), "ws://localhost:8080/ws/page");
    }
}
