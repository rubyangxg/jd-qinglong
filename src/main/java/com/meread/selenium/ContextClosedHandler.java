package com.meread.selenium;

import com.meread.selenium.bean.NodeStatus;
import com.meread.selenium.bean.SlotStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ContextClosedHandler implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        WebDriverFactory webDriverFactory = context.getBean(WebDriverFactory.class);

        webDriverFactory.stopSchedule = true;
        while (webDriverFactory.runningSchedule) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("wait WebDriverFactory schedule destroy...");
        }

        List<NodeStatus> status = webDriverFactory.getGridStatus();
        for (NodeStatus ns : status) {
            String uri = ns.getUri();
            List<SlotStatus> slotStatus = ns.getSlotStatus();
            for (SlotStatus ss : slotStatus) {
                String sessionId = ss.getSessionId();
                if (sessionId != null) {
                    log.info("destroy chrome : " + uri + " --> " + sessionId);
                    webDriverFactory.closeSession(uri, sessionId);
                }
            }
        }
    }
}