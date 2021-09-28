package com.meread.selenium;

import com.alibaba.fastjson.JSONObject;
import com.meread.selenium.bean.SelenoidStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class ContextClosedHandler implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        WebDriverManager webDriverManager = context.getBean(WebDriverManager.class);

        webDriverManager.stopSchedule = true;
        while (webDriverManager.runningSchedule) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("wait WebDriverFactory schedule destroy...");
        }

        SelenoidStatus status = webDriverManager.getGridStatus();
        Map<String, JSONObject> sessions = status.getSessions();
        if (sessions != null) {
            for (String sessionId : sessions.keySet()) {
                if (sessionId != null) {
                    log.info("destroy chrome " + sessionId);
                    webDriverManager.closeSession(sessionId);
                }
            }
        }
        webDriverManager.cleanDockerContainer();
    }
}