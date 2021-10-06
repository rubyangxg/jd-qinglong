package com.meread.selenium;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.amihaiemil.docker.Container;
import com.amihaiemil.docker.Containers;
import com.amihaiemil.docker.UnixDocker;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.CommonAttributes;
import com.meread.selenium.util.WebDriverOpCallBack;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.LocalStorage;
import org.openqa.selenium.remote.RemoteExecuteMethod;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.html5.RemoteWebStorage;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Component
@Slf4j
public class WSManager implements DisposableBean {

    @Autowired
    private JDService jdService;

    @Autowired
    private WebDriverManager driverManager;

    public volatile boolean runningSchedule = false;
    public volatile boolean stopSchedule = false;

    public static final Map<String, Map<String, WebSocketSession>>
            socketSessionPool = new ConcurrentHashMap<>();

    public static final Map<String, JDScreenBean.PageStatus> lastPageStatus = new ConcurrentHashMap<>();

    public static synchronized void addNew(WebSocketSession session) {
        String webSocketSessionId = session.getId();
        String httpSessionId = (String) session.getAttributes().get(CommonAttributes.SESSION_ID);
        log.info("WebSocket connection established, webSocketSessionId = {} httpSessionId = {} ConnectCount = {}", webSocketSessionId, httpSessionId, WSManager.getConnectionCount());
        Map<String, WebSocketSession> socketSessionMap = socketSessionPool.get(httpSessionId);
        if (socketSessionMap == null) {
            socketSessionMap = new HashMap<>();
        }
        socketSessionMap.put(webSocketSessionId, session);
        socketSessionPool.put(httpSessionId, socketSessionMap);
    }

    public static synchronized void removeOld(WebSocketSession session) {
        String webSocketSessionId = session.getId();
        String httpSessionId = (String) session.getAttributes().get(CommonAttributes.SESSION_ID);
        Map<String, WebSocketSession> socketSessionMap = socketSessionPool.get(httpSessionId);
        if (socketSessionMap != null) {
            WebSocketSession remove = socketSessionMap.remove(webSocketSessionId);
            if (remove != null && socketSessionMap.isEmpty()) {
                socketSessionPool.remove(httpSessionId);
            }
        }
    }

    public static int getConnectionCount() {
        return socketSessionPool.size();
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 1000)
    public void heartbeat() {
        runningSchedule = true;
        if (!stopSchedule) {
            doPushScreen();
        }
        runningSchedule = false;
    }

    private void doPushScreen() {
        for (String httpSessionId : socketSessionPool.keySet()) {
            Map<String, WebSocketSession> socketSessionMap = socketSessionPool.get(httpSessionId);
            MyChromeClient myChromeClient = driverManager.getCacheMyChromeClient(httpSessionId);
            if (myChromeClient != null) {
                JDScreenBean screen = jdService.getScreen(myChromeClient);
                JDScreenBean.PageStatus pageStatus = lastPageStatus.get(httpSessionId);
                if (pageStatus == null || pageStatus != screen.getPageStatus()) {
                    lastPageStatus.put(httpSessionId, screen.getPageStatus());
                    for (WebSocketSession socketSession : socketSessionMap.values()) {
                        try {
                            socketSession.sendMessage(new TextMessage(JSON.toJSONString(screen)));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    @Override
    public void destroy() throws Exception {
        for (Map<String, WebSocketSession> socketSessionMap : socketSessionPool.values()) {
            for (WebSocketSession socketSession : socketSessionMap.values()) {
                socketSession.close();
            }
        }
    }

}
