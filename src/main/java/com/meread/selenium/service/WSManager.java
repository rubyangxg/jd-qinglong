package com.meread.selenium.service;

import com.alibaba.fastjson.JSON;
import com.meread.selenium.bean.*;
import com.meread.selenium.util.CommonAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WSManager implements DisposableBean {

    @Autowired
    private JDService jdService;

    @Autowired
    private BaseWebDriverManager driverManager;

    public volatile boolean runningSchedule = false;
    public volatile boolean stopSchedule = false;

    public final Map<String, Map<String, WebSocketSession>>
            socketSessionPool = new ConcurrentHashMap<>();

    public Map<String, JDScreenBean> lastPageStatus = new ConcurrentHashMap<>();

    public synchronized void addNew(WebSocketSession session) {
        String webSocketSessionId = session.getId();
        String httpSessionId = (String) session.getAttributes().get(CommonAttributes.SESSION_ID);
        String jdLoginType = (String) session.getAttributes().get(CommonAttributes.JD_LOGIN_TYPE);
        Map<String, WebSocketSession> socketSessionMap = socketSessionPool.get(httpSessionId);
        if (socketSessionMap == null) {
            socketSessionMap = new HashMap<>();
        }
        socketSessionMap.put(webSocketSessionId, session);
        socketSessionPool.put(httpSessionId, socketSessionMap);
        MyChromeClient cacheMyChromeClient = driverManager.getCacheMyChromeClient(httpSessionId);
        if (cacheMyChromeClient == null) {
            driverManager.createNewMyChromeClient(httpSessionId, LoginType.WEB, JDLoginType.valueOf(jdLoginType));
        }
    }

    public synchronized void removeOld(WebSocketSession session) {
        String webSocketSessionId = session.getId();
        String httpSessionId = (String) session.getAttributes().get(CommonAttributes.SESSION_ID);
        Map<String, WebSocketSession> socketSessionMap = socketSessionPool.get(httpSessionId);
        if (socketSessionMap != null) {
            WebSocketSession remove = socketSessionMap.remove(webSocketSessionId);
            if (remove != null && socketSessionMap.isEmpty()) {
                socketSessionPool.remove(httpSessionId);
                lastPageStatus.remove(httpSessionId);
                MyChromeClient cacheMyChromeClient = driverManager.getCacheMyChromeClient(httpSessionId);
                if (cacheMyChromeClient != null) {
                    driverManager.releaseWebDriver(cacheMyChromeClient.getChromeSessionId(),false);
                }
            }
        }
    }

    public int getConnectionCount() {
        return socketSessionPool.size();
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 1000)
    public void heartbeat() {
        runningSchedule = true;
        if (!stopSchedule) {
            doPushScreen();
        }
        runningSchedule = false;
        Set<String> onlineUserTrackIds = socketSessionPool.keySet();
        for (MyChrome chrome : driverManager.getChromes().values()) {
            if (chrome.getUserTrackId() != null && !onlineUserTrackIds.contains(chrome.getUserTrackId())) {
                driverManager.releaseWebDriver(chrome.getChromeSessionId(),false);
            }
        }
        for (MyChromeClient client : driverManager.getClients().values()) {
            if (client.getUserTrackId() != null && !onlineUserTrackIds.contains(client.getUserTrackId())) {
                driverManager.releaseWebDriver(client.getChromeSessionId(),false);
            }
        }
    }

    private void doPushScreen() {
        Iterator<Map.Entry<String, Map<String, WebSocketSession>>> it = socketSessionPool.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Map<String, WebSocketSession>> entry = it.next();
            String httpSessionId = entry.getKey();
            Map<String, WebSocketSession> socketSessionMap = entry.getValue();
            MyChromeClient myChromeClient = driverManager.getCacheMyChromeClient(httpSessionId);
            if (myChromeClient != null) {
                JDScreenBean screen = jdService.getScreen(myChromeClient);

                if (myChromeClient.isExpire()) {
                    driverManager.releaseWebDriver(myChromeClient.getChromeSessionId(),false);
                    for (WebSocketSession socketSession : socketSessionMap.values()) {
                        try {
                            socketSession.sendMessage(new TextMessage(JSON.toJSONString(new JDScreenBean("", "", JDScreenBean.PageStatus.SESSION_EXPIRED))));
                            socketSession.close();
                            it.remove();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                if (!myChromeClient.isPushedXDD()) {
                    if (screen.getPageStatus().equals(JDScreenBean.PageStatus.SUCCESS_CK)) {
                        log.info("已经获取到ck了 " + myChromeClient + ", ck = " + screen.getCk());
                        String xddRes = jdService.doXDDNotify(screen.getCk().toString());
                        log.info("doXDDNotify res = " + xddRes);
                        myChromeClient.setPushedXDD(true);
                    }
                }
                JDScreenBean oldScreen = lastPageStatus.get(httpSessionId);
                long diff = Integer.MAX_VALUE;
                if (oldScreen != null) {
                    diff = System.currentTimeMillis() - oldScreen.getSnapshotTime();
                }
                int threshold = 2000;
                if (CommonAttributes.debug) {
                    threshold = 0;
                }
                if (oldScreen == null || oldScreen.getPageStatus() == null || oldScreen.getPageStatus() != screen.getPageStatus() || screen.getPageStatus() == JDScreenBean.PageStatus.REQUIRE_VERIFY || diff > threshold) {
                    lastPageStatus.put(httpSessionId, screen);
                    for (WebSocketSession socketSession : socketSessionMap.values()) {
                        Object obj = socketSession.getAttributes().get("push");
                        if (obj != null && !((boolean) obj)) {
                            continue;
                        }
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

    public Map<String, JDScreenBean> getLastPageStatus() {
        return lastPageStatus;
    }
}
